package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(arguments, player.getLocation());
        if (radiusArg == null) {
            radiusArg = new RadiusArg(player.getLocation(), 10);
            plugin.getLangManager().sendMessage(player, "messages.specifyRadius");
        }
        List<ActionType> actionTypes = Arrays.asList(ActionType.BLOCK_BREAK, ActionType.BLOCK_PLACE, ActionType.INVENTORY_TRANSACTION);

        executeRestore(player, sender, timeArg, radiusArg, actionTypes);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length >= 1) {
            String currentArg = arguments[arguments.length - 1];

            if (currentArg.startsWith("t:") || currentArg.startsWith("time:")) {
                return Arrays.asList("t:1h", "t:1d", "t:1w", "t:1mo");
            }

            if (currentArg.startsWith("r:") || currentArg.startsWith("radius:")) {
                return Arrays.asList("r:10", "r:20", "r:30", "r:40", "r:50");
            }
        }

        return Arrays.asList("t:1h", "r:10");
    }

    public void executeRestore(Player player, CommandSender sender, TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes) {
        final int BATCH_SIZE = 1000;
        final int DELAY_TICKS = 2;

        plugin.getProtectDatabase().countRestoreActions(timeArg, radiusArg, actionTypes).thenAccept(totalLogs -> {
            if (totalLogs == 0) {
                plugin.getLangManager().sendMessage(player, "messages.noLogs");
                return;
            }

            player.sendMessage("§aIniciando rollback de §e" + totalLogs + "§a logs en lotes de §e" + BATCH_SIZE + "§a...");

            processBatchRestore(sender, timeArg, radiusArg, actionTypes, 0, totalLogs, BATCH_SIZE, DELAY_TICKS);

        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.error");
            error.printStackTrace();
            return null;
        });
    }

    private void processBatchRestore(CommandSender sender, TimeArg timeArg, RadiusArg radiusArg, List<ActionType> actionTypes, int currentSkip, long totalLogs, int batchSize, int delayTicks) {
        if (currentSkip >= totalLogs) {
            sender.sendMessage("§a¡Rollback completado! Se procesaron §e" + totalLogs + "§a logs.");
            return;
        }

        plugin.getProtectDatabase().getRestoreActions(timeArg, radiusArg, actionTypes, currentSkip, batchSize)
            .thenAccept(callbackLookup -> {
                Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();
                int processedInBatch = groupedLogs.values().stream().mapToInt(Set::size).sum();

                if (processedInBatch > 0) {
                    plugin.getStellarTaskHook(() -> {
                        plugin.getRestoreManager().rollback(sender, groupedLogs);

                        int totalProcessed = currentSkip + processedInBatch;
                        double progress = (totalProcessed * 100.0) / totalLogs;
                        sender.sendMessage(String.format("§bProgreso: §e%.1f%% §7(§e%d§7/§e%d§7)",
                            progress, totalProcessed, totalLogs));

                        plugin.getStellarTaskHook(() -> processBatchRestore(sender, timeArg, radiusArg, actionTypes,
                            currentSkip + batchSize, totalLogs, batchSize, delayTicks)).runTask(delayTicks);
                    }).runTask();
                } else {
                    sender.sendMessage("§a¡Rollback completado! Se procesaron §e" + currentSkip + "§a logs.");
                }
            })
            .exceptionally(error -> {
                sender.sendMessage("§cError en lote " + (currentSkip / batchSize + 1) + ": " + error.getMessage());
                error.printStackTrace();
                return null;
            });
    }

}
