package io.github.insideranh.stellarprotect.commands.arguments.lookups;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.PageArg;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.LookupSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NextLookupArgument extends StellarArgument {

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

        LookupSession lookupSession = playerProtect.getLookupSession();
        if (lookupSession == null) return;

        PageArg pageArg = ArgumentsParser.parsePage(arguments);

        if (playerProtect.getNextLookup() > System.currentTimeMillis()) {
            plugin.getLangManager().sendMessage(sender, "messages.waitingForLookup");
            return;
        }

        playerProtect.setNextLookup(System.currentTimeMillis() + 5000L);
        playerProtect.getPosibleLogs().clear();

        plugin.getLangManager().sendMessage(sender, "messages.loadingLookup");

        plugin.getProtectDatabase().getLogs(lookupSession.getDatabaseFilters(), false, pageArg.getSkip(), pageArg.getLimit()).thenAccept(callbackLookup -> {
            Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();

            long total = (long) Math.ceil((double) callbackLookup.getTotal() / pageArg.getPerPage());

            for (Map.Entry<LocationCache, Set<LogEntry>> entry : groupedLogs.entrySet()) {
                LocationCache location = entry.getKey();
                Set<LogEntry> logs = entry.getValue();

                for (LogEntry logEntry : logs) {
                    ActionType actionType = ActionType.getById(logEntry.getActionType());
                    if (actionType == null) continue;

                    plugin.getInspectHandler().processLogEntry(player, logEntry);
                }

                plugin.getProtectNMS().sendActionTitle(player, plugin.getLangManager().get("messages.actions.location"), "§fClick to teleport!", "/stellarprotect t " + WorldUtils.getWorld(location.getWorldId()) + " " + location.getX() + " " + location.getY() + " " + location.getZ(), text -> text.replace("<location>", WorldUtils.getFormatedLocation(location)));
            }

            plugin.getProtectNMS().sendPageButtons(player,
                plugin.getLangManager().get("messages.pagesNav"),
                plugin.getLangManager().get("messages.clickPage"),
                pageArg.getPage(),
                pageArg.getPerPage(),
                (int) total);

            playerProtect.setNextLookup(System.currentTimeMillis() + 500L);
        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.noLogs");

            playerProtect.setNextLookup(System.currentTimeMillis() + 500L);

            error.printStackTrace();
            Debugger.debugLog("Error on lookup: " + error.getMessage());
            return null;
        });

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>();
    }

}
