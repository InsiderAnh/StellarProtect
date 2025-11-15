package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import io.github.insideranh.stellarprotect.cache.ItemsCache;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.data.RestoreSession;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestoreArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().sendMessage(sender, "messages.onlyPlayer");
            return;
        }
        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        HashTagsArg hashTagsArg = new HashTagsArg(arguments);
        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(player, arguments, player.getLocation());

        if (radiusArg == null) {
            radiusArg = new RadiusArg(player.getLocation(), 10, -1);
            plugin.getLangManager().sendMessage(player, "messages.specifyRadius");
        }

        List<ActionType> actionTypesArg = ArgumentsParser.parseActionTypes(arguments);
        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);
        Map<String, List<String>> includesMap = ArgumentsParser.parseIncludeMaterials(arguments);
        Map<String, List<String>> excludesMap = ArgumentsParser.parseExcludeMaterials(arguments);

        List<ActionType> actionTypes;
        if (actionTypesArg.isEmpty()) {
            actionTypes = new LinkedList<>(Arrays.asList(
                ActionType.BLOCK_BREAK,
                ActionType.BLOCK_PLACE,
                ActionType.BUCKET_EMPTY,
                ActionType.BUCKET_FILL,
                ActionType.BLOCK_SPREAD,
                ActionType.INVENTORY_TRANSACTION
            ));

            if (hashTagsArg.isEntities()) {
                actionTypes.add(ActionType.KILL_ENTITY);
            }
        } else {
            actionTypes = actionTypesArg;
        }

        ItemsCache itemsCache = StellarProtect.getInstance().getItemsManager().getItemCache();
        BlocksCache blocksCache = StellarProtect.getInstance().getBlocksManager().getBlocksCache();

        RadiusArg finalRadiusArg = radiusArg;
        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters databaseFilters = new DatabaseFilters();
            databaseFilters.setTimeFilter(timeArg);
            databaseFilters.setRadiusFilter(finalRadiusArg);
            databaseFilters.setActionTypesFilter(actionTypes.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
            databaseFilters.setUserFilters(usersArg);
            databaseFilters.setAllIncludeFilters(itemsCache.findIdsByTypeNameContains(includesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setAllExcludeFilters(itemsCache.findIdsByTypeNameContains(excludesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setIncludeBlockFilters(blocksCache.findIdsByTypeNameContains(includesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setExcludeBlockFilters(blocksCache.findIdsByTypeNameContains(excludesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setIncludeMaterialFilters(itemsCache.findIdsContains(includesMap));
            databaseFilters.setExcludeMaterialFilters(itemsCache.findIdsContains(excludesMap));

            if (hashTagsArg.isSession()) {
                RestoreSession session = new RestoreSession(player, databaseFilters, hashTagsArg.isVerbose(), hashTagsArg.isSilent());
                playerProtect.setRestoreSession(session);

                plugin.getRestoreSessionManager().showRestoreSession(session);
                return;
            }

            executeRestore(player, hashTagsArg.isPreview(), hashTagsArg.isVerbose(), hashTagsArg.isSilent(), databaseFilters);
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length >= 1) {
            String currentArg = arguments[arguments.length - 1].toLowerCase();

            if (currentArg.startsWith("t:") || currentArg.startsWith("time:")) {
                return Arrays.asList("t:1h", "t:1d", "t:1w", "t:1mo", "t:1y", "t:1h-2h", "t:1d-7d", "t:1mo-2mo");
            }

            if (currentArg.startsWith("r:") || currentArg.startsWith("radius:")) {
                return Arrays.asList("r:10", "r:20", "r:50", "r:#world", "r:10,10,10");
            }

            if (currentArg.startsWith("a:") || currentArg.startsWith("action:")) {
                return Arrays.asList("a:block_break", "a:block_place", "a:inventory_transaction", "a:kill_entity", "a:block_spread");
            }

            if (currentArg.startsWith("u:") || currentArg.startsWith("users:")) {
                return Arrays.asList("u:player1", "u:player1,player2", "u:=fire", "u:=natural");
            }

            if (currentArg.startsWith("i:") || currentArg.startsWith("include:")) {
                return Arrays.asList("i:diamond", "i:stone,dirt", "i:ore");
            }

            if (currentArg.startsWith("e:") || currentArg.startsWith("exclude:")) {
                return Arrays.asList("e:air", "e:stone", "e:dirt");
            }

            return Stream.of("t:1h", "t:1d", "t:1w", "t:1mo", "r:10", "a:", "u:", "i:", "e:", "mi:", "me:", "#session", "#preview", "#verbose", "#silent", "#count", "#entities")
                .filter(name -> name.contains(currentArg))
                .collect(Collectors.toList());
        }

        return Arrays.asList("t:1h", "t:1d", "t:1w", "t:1mo", "r:10", "a:", "u:", "i:", "e:", "#session", "#preview", "#verbose", "#silent", "#count", "#entities");
    }

    public void executeRestore(Player player, boolean preview, boolean verbose, boolean silent, DatabaseFilters filters) {
        final int BATCH_SIZE = 1000;
        final int DELAY_TICKS = 2;

        plugin.getProtectDatabase().countRestoreActions(filters).thenAccept(totalLogs -> {
            if (totalLogs == 0) {
                plugin.getLangManager().sendMessage(player, "messages.noLogs");
                return;
            }

            if (!silent) {
                if (verbose) {
                    player.sendMessage(plugin.getLangManager().get("messages.rollback.start-verbose").replace("<total>", String.valueOf(totalLogs)).replace("<batch>", String.valueOf(BATCH_SIZE)));
                } else {
                    player.sendMessage(plugin.getLangManager().get("messages.rollback.start").replace("<total>", String.valueOf(totalLogs)).replace("<batch>", String.valueOf(BATCH_SIZE)));
                }
            }

            processBatchRestore(player, preview, verbose, silent, filters, 0, totalLogs, BATCH_SIZE, DELAY_TICKS);
        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.error");
            error.printStackTrace();
            return null;
        });
    }

    private void processBatchRestore(Player player, boolean preview, boolean verbose, boolean silent, DatabaseFilters filters, int currentSkip, long totalLogs, int batchSize, int delayTicks) {
        if (currentSkip >= totalLogs) {
            if (!silent) {
                player.sendMessage(plugin.getLangManager().get("messages.rollback.success").replace("<current>", String.valueOf(currentSkip)).replace("<total>", String.valueOf(totalLogs)));
            }
            return;
        }

        plugin.getProtectDatabase().getRestoreActions(filters, currentSkip, batchSize)
            .thenAccept(callbackLookup -> {
                Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();
                int processedInBatch = groupedLogs.values().stream().mapToInt(Set::size).sum();

                if (processedInBatch > 0) {
                    plugin.getStellarTaskHook(() -> {
                        if (preview) {
                            plugin.getRestoreManager().preview(player, groupedLogs, verbose, silent);
                        } else {
                            plugin.getRestoreManager().rollback(player, groupedLogs, verbose, silent);
                        }

                        int totalProcessed = currentSkip + processedInBatch;
                        double progress = (totalProcessed * 100.0) / totalLogs;

                        if (!silent) {
                            if (preview) {
                                if (verbose) {
                                    player.sendMessage(plugin.getLangManager().get("messages.rollback.preview-verbose").replace("<progress>", String.valueOf(Math.round(progress))).replace("<now>", String.valueOf(totalProcessed)).replace("<total>", String.valueOf(totalLogs)));
                                } else {
                                    player.sendMessage(plugin.getLangManager().get("messages.rollback.preview").replace("<progress>", String.valueOf(Math.round(progress))).replace("<now>", String.valueOf(totalProcessed)).replace("<total>", String.valueOf(totalLogs)));
                                }
                            } else {
                                if (verbose) {
                                    player.sendMessage(plugin.getLangManager().get("messages.rollback.progress-verbose").replace("<progress>", String.valueOf(Math.round(progress))).replace("<now>", String.valueOf(totalProcessed)).replace("<total>", String.valueOf(totalLogs)));
                                } else {
                                    player.sendMessage(plugin.getLangManager().get("messages.rollback.progress").replace("<progress>", String.valueOf(Math.round(progress))).replace("<now>", String.valueOf(totalProcessed)).replace("<total>", String.valueOf(totalLogs)));
                                }
                            }
                        }

                        plugin.getStellarTaskHook(() -> processBatchRestore(player, preview, verbose, silent, filters, currentSkip + batchSize, totalLogs, batchSize, delayTicks)).runTask(delayTicks);
                    }).runTask();
                } else {
                    player.sendMessage(plugin.getLangManager().get("messages.rollback.success").replace("<current>", String.valueOf(currentSkip)).replace("<total>", String.valueOf(totalLogs)));
                }
            })
            .exceptionally(error -> {
                player.sendMessage(plugin.getLangManager().get("messages.rollback.error").replace("<section>", String.valueOf(currentSkip / batchSize + 1)).replace("<error>", error.getMessage()));
                error.printStackTrace();
                return null;
            });
    }

}