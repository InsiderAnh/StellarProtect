package io.github.insideranh.stellarprotect.commands.arguments.lookups;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import io.github.insideranh.stellarprotect.cache.ItemsCache;
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
import java.util.stream.Collectors;

public class LookupArgument extends StellarArgument {

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

        PageArg pageArg = ArgumentsParser.parsePage(arguments);
        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(player, arguments, player.getLocation());
        List<ActionType> actionTypesArg = ArgumentsParser.parseActionTypes(arguments);
        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);

        Map<String, List<String>> includesMap = ArgumentsParser.parseIncludeMaterials(arguments);
        Map<String, List<String>> excludesMap = ArgumentsParser.parseExcludeMaterials(arguments);

        if (playerProtect.getNextLookup() > System.currentTimeMillis()) {
            plugin.getLangManager().sendMessage(sender, "messages.waitingForLookup");
            return;
        }
        playerProtect.setNextLookup(System.currentTimeMillis() + 5000L);

        ItemsCache itemsCache = StellarProtect.getInstance().getItemsManager().getItemCache();
        BlocksCache blocksCache = StellarProtect.getInstance().getBlocksManager().getBlocksCache();
        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters databaseFilters = new DatabaseFilters();
            databaseFilters.setTimeFilter(timeArg);
            databaseFilters.setRadiusFilter(radiusArg);
            databaseFilters.setPageFilter(pageArg);
            databaseFilters.setAllIncludeFilters(itemsCache.findIdsByTypeNameContains(includesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setAllExcludeFilters(itemsCache.findIdsByTypeNameContains(excludesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setIncludeBlockFilters(blocksCache.findIdsByTypeNameContains(includesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setExcludeBlockFilters(blocksCache.findIdsByTypeNameContains(excludesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            databaseFilters.setIncludeMaterialFilters(itemsCache.findIdsContains(includesMap));
            databaseFilters.setExcludeMaterialFilters(itemsCache.findIdsContains(excludesMap));

            databaseFilters.setActionTypesFilter(actionTypesArg.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
            databaseFilters.setUserFilters(usersArg);

            playerProtect.getPosibleLogs().clear();
            playerProtect.setInspectSession(null);
            playerProtect.setLookupSession(new LookupSession(pageArg, databaseFilters, 0, 10));

            plugin.getLangManager().sendMessage(sender, "messages.actions.lookup");

            plugin.getProtectDatabase().getLogs(databaseFilters, databaseFilters.isIgnoreCache(), pageArg.getSkip(), pageArg.getLimit()).thenAccept(callbackLookup -> {
                Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();
                long total = (long) Math.ceil((double) callbackLookup.getTotal() / pageArg.getPerPage());

                playerProtect.setNextLookup(System.currentTimeMillis() + 500L);

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
            }).exceptionally(error -> {
                plugin.getLangManager().sendMessage(player, "messages.noLogs");

                playerProtect.setNextLookup(System.currentTimeMillis() + 500L);
                error.printStackTrace();
                Debugger.debugLog("Error on lookup: " + error.getMessage());
                return null;
            });
        });
    }

}