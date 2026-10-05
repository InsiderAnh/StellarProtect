package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import io.github.insideranh.stellarprotect.cache.ItemsCache;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ExportArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        HashTagsArg hashTagsArg = new HashTagsArg(arguments);
        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(sender instanceof Player ? (Player) sender : null, arguments, sender instanceof Player ? ((Player) sender).getLocation() : null);
        List<ActionType> actionTypesArg = ArgumentsParser.parseActionTypes(arguments);
        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);
        Map<String, List<String>> includesMap = ArgumentsParser.parseIncludeMaterials(arguments);
        Map<String, List<String>> excludesMap = ArgumentsParser.parseExcludeMaterials(arguments);

        String fmt = hashTagsArg.isExportCsv() ? "csv" : "json";
        List<ActionType> actionTypes = actionTypesArg.isEmpty()
                ? Arrays.asList(ActionType.BLOCK_BREAK, ActionType.BLOCK_PLACE, ActionType.BUCKET_EMPTY, ActionType.BUCKET_FILL, ActionType.BLOCK_SPREAD, ActionType.INVENTORY_TRANSACTION)
                : actionTypesArg;

        ItemsCache itemsCache = plugin.getItemsManager().getItemCache();
        BlocksCache blocksCache = plugin.getBlocksManager().getBlocksCache();

        RadiusArg finalRadiusArg = radiusArg;
        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters filters = new DatabaseFilters();
            filters.setTimeFilter(timeArg);
            filters.setRadiusFilter(finalRadiusArg);
            filters.setActionTypesFilter(actionTypes.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
            filters.setUserFilters(usersArg);
            filters.setAllIncludeFilters(itemsCache.findIdsByTypeNameContains(includesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setAllExcludeFilters(itemsCache.findIdsByTypeNameContains(excludesArg, ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setIncludeBlockFilters(blocksCache.findIdsByTypeNameContains(includesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            filters.setExcludeBlockFilters(blocksCache.findIdsByTypeNameContains(excludesArg, BlocksCache.FieldType.LOWER_TYPE_NAME));
            filters.setIncludeMaterialFilters(itemsCache.findIdsContains(includesMap));
            filters.setExcludeMaterialFilters(itemsCache.findIdsContains(excludesMap));

            plugin.getProtectDatabase().countRestoreActions(filters).thenAccept(total -> {
                if (total == 0) {
                    plugin.getLangManager().sendMessage(sender, "messages.noLogs");
                    return;
                }
                long bounded = Math.min(total, 50000L);
                plugin.getProtectDatabase().getRestoreActions(filters, 0, (int) bounded).thenAccept(cb -> {
                    exportToFile(sender, cb.getLogs(), fmt);
                });
            });
        });
    }

    private void exportToFile(CommandSender sender, Map<LocationCache, Set<LogEntry>> grouped, String fmt) {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        File out = new File(folder, "logs-export-" + System.currentTimeMillis() + "." + fmt);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            if ("csv".equals(fmt)) {
                w.write("id,player_id,world_id,x,y,z,action_type,created_at,data");
                w.newLine();
                for (Set<LogEntry> set : grouped.values()) {
                    for (LogEntry e : set) {
                        w.write(e.getId() + "," + e.getPlayerId() + "," + e.getWorldId() + "," + e.getX() + "," + e.getY() + "," + e.getZ() + "," + e.getActionType() + "," + e.getCreatedAt() + "," + escapeCsv(e.getDataString()));
                        w.newLine();
                    }
                }
            } else {
                w.write("{ \"entries\": [");
                w.newLine();
                boolean first = true;
                for (Set<LogEntry> set : grouped.values()) {
                    for (LogEntry e : set) {
                        if (!first) w.write(",");
                        first = false;
                        w.write("  { \"id\":" + e.getId() + ",\"player_id\":" + e.getPlayerId() + ",\"world_id\":" + e.getWorldId() + ",\"x\":" + e.getX() + ",\"y\":" + e.getY() + ",\"z\":" + e.getZ() + ",\"action_type\":" + e.getActionType() + ",\"created_at\":" + e.getCreatedAt() + ",\"data\":\"" + escapeJson(e.getDataString()) + "\" }");
                        w.newLine();
                    }
                }
                w.write("] }");
                w.newLine();
            }
            sender.sendMessage("§aExportado a: §f" + out.getAbsolutePath());
        } catch (Exception e) {
            sender.sendMessage("§cError exportando: §f" + e.getMessage());
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return Arrays.asList("#export-csv", "#export-json", "t:1h", "r:10", "a:");
    }

}