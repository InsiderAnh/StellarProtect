package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ItemRestoreArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull org.bukkit.command.CommandSender sender, String[] arguments) {
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

        List<ActionType> actionTypes = Arrays.asList(ActionType.DROP_ITEM, ActionType.PICKUP_ITEM, ActionType.CRAFT, ActionType.ENCHANT, ActionType.SMITH, ActionType.CONSUME, ActionType.BREWING);
        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);

        RadiusArg finalRadiusArg = radiusArg;
        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters filters = new DatabaseFilters();
            filters.setTimeFilter(timeArg);
            filters.setRadiusFilter(finalRadiusArg);
            filters.setActionTypesFilter(actionTypes.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));
            filters.setUserFilters(usersArg);
            filters.setIncludeMaterialFilters(plugin.getItemsManager().getItemCache().findIdsByTypeNameContains(includesArg, io.github.insideranh.stellarprotect.cache.ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setExcludeMaterialFilters(plugin.getItemsManager().getItemCache().findIdsByTypeNameContains(excludesArg, io.github.insideranh.stellarprotect.cache.ItemsCache.FieldType.LOWER_TYPE_NAME));
            filters.setMinAmount(ArgumentsParser.parseMinAmount(arguments));
            filters.setMaxAmount(ArgumentsParser.parseMaxAmount(arguments));

            plugin.getProtectDatabase().getRestoreActions(filters, 0, 10000).thenAccept(cb -> {
                int restored = 0;
                int dropped = 0;
                for (Map.Entry<LocationCache, Set<LogEntry>> entry : cb.getLogs().entrySet()) {
                    for (LogEntry log : entry.getValue()) {
                        if (!(log instanceof PlayerItemLogEntry)) continue;
                        PlayerItemLogEntry itemLog = (PlayerItemLogEntry) log;
                        ItemStack stack = plugin.getItemsManager().getItemTemplate(itemLog.getItemReferenceId()).getBukkitItem().clone();
                        if (stack == null) continue;
                        stack.setAmount(itemLog.getAmount());

                        if (isUndoOf(itemLog, hashTagsArg)) {
                            dropped += giveOrDrop(player, stack, false);
                        } else {
                            restored += giveOrDrop(player, stack, true);
                        }
                    }
                }
                player.sendMessage("§aRestaurados: §f" + restored + " §7items.");
                player.sendMessage("§eDevueltos al suelo: §f" + dropped);
            });
        });
    }

    private int giveOrDrop(Player player, ItemStack stack, boolean give) {
        PlayerInventory inv = player.getInventory();
        HashMap<Integer, ItemStack> leftover;
        if (give) {
            leftover = inv.addItem(stack);
        } else {
            leftover = new HashMap<>();
            leftover.put(0, stack);
            leftover = inv.removeItem(stack);
        }
        Location loc = player.getLocation();
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(loc, drop);
        }
        return stack.getAmount();
    }

    private boolean isUndoOf(LogEntry log, HashTagsArg hashTagsArg) {
        return hashTagsArg.isUndoMode() || hashTagsArg.isRedo();
    }

    @Override
    public List<String> onTabComplete(@NotNull org.bukkit.command.CommandSender sender, String[] arguments) {
        return java.util.Arrays.asList("t:1h", "t:1d", "t:30m", "r:10", "i:diamond", "e:stone", "b:64", "u:player", "#undo");
    }

}