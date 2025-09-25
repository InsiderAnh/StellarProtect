package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.HashTagsArg;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
        List<ActionType> actionTypes = Arrays.asList(ActionType.BLOCK_BREAK, ActionType.BLOCK_PLACE, ActionType.BUCKET_EMPTY, ActionType.BUCKET_FILL, ActionType.BLOCK_SPREAD, ActionType.INVENTORY_TRANSACTION);

        executeRestore(player, hashTagsArg.isPreview(), hashTagsArg.isVerbose(), hashTagsArg.isSilent(), timeArg, radiusArg, actionTypes);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length >= 1) {
            String currentArg = arguments[arguments.length - 1];

            if (currentArg.startsWith("t:") || currentArg.startsWith("time:")) {
                return Arrays.asList("t:1h-2h", "t:1d", "t:1w", "t:1mo");
            }

            if (currentArg.startsWith("r:") || currentArg.startsWith("radius:")) {
                return Arrays.asList("r:10", "r:20", "r:#world", "r:10,10,10");
            }
        }

        return Arrays.asList("t:1h", "r:10", "#session", "#preview", "#verbose", "#silent", "#count");
    }

    public void executeRestore(Player player, boolean preview, boolean verbose, boolean silent, TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes) {
        final int BATCH_SIZE = 1000;
        final int DELAY_TICKS = 2;

        plugin.getProtectDatabase().countRestoreActions(timeArg, radiusArg, actionTypes).thenAccept(totalLogs -> {
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

            processBatchRestore(player, preview, verbose, silent, timeArg, radiusArg, actionTypes, 0, totalLogs, BATCH_SIZE, DELAY_TICKS);
        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.error");
            error.printStackTrace();
            return null;
        });
    }

    private void processBatchRestore(Player player, boolean preview, boolean verbose, boolean silent, TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes, int currentSkip, long totalLogs, int batchSize, int delayTicks) {
        if (currentSkip >= totalLogs) {
            if (!silent) {
                player.sendMessage(plugin.getLangManager().get("messages.rollback.success").replace("<current>", String.valueOf(currentSkip)).replace("<total>", String.valueOf(totalLogs)));
            }
            return;
        }

        plugin.getProtectDatabase().getRestoreActions(timeArg, radiusArg, actionTypes, currentSkip, batchSize)
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

                        plugin.getStellarTaskHook(() -> processBatchRestore(player, preview, verbose, silent, timeArg, radiusArg, actionTypes, currentSkip + batchSize, totalLogs, batchSize, delayTicks)).runTask(delayTicks);
                    }).runTask();
                } else {
                    if (!silent) {
                        player.sendMessage(plugin.getLangManager().get("messages.rollback.success").replace("<current>", String.valueOf(currentSkip)).replace("<total>", String.valueOf(totalLogs)));
                    }
                }
            })
            .exceptionally(error -> {
                player.sendMessage(plugin.getLangManager().get("messages.rollback.error").replace("<section>", String.valueOf(currentSkip / batchSize + 1)).replace("<error>", error.getMessage()));
                error.printStackTrace();
                return null;
            });
    }

}