package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.*;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import io.github.insideranh.stellarprotect.cache.ItemsCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.InventoryRollbackSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InventoryRollbackArgument extends StellarArgument {

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

        List<String> includesArg = ArgumentsParser.parseIncludesMaterials(arguments);
        List<String> excludesArg = ArgumentsParser.parseExcludesMaterials(arguments);
        Map<String, List<String>> includesMap = ArgumentsParser.parseIncludeMaterials(arguments);
        Map<String, List<String>> excludesMap = ArgumentsParser.parseExcludeMaterials(arguments);

        List<ActionType> actionTypes = Collections.singletonList(ActionType.INVENTORY_TRANSACTION);

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

            InventoryRollbackSession session = new InventoryRollbackSession(
                player,
                databaseFilters,
                hashTagsArg.isVerbose(),
                hashTagsArg.isSilent()
            );

            playerProtect.setInventoryRollbackSession(session);

            if (!hashTagsArg.isSilent()) {
                plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.sessionStarted");
                player.sendMessage("§aHaz click en un contenedor (cofre, barril, shulker, etc.) para restaurar su contenido.");
                player.sendMessage("§eUsa §f/sp irs toggle §epara desactivar el modo.");
            }
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length >= 1) {
            String currentArg = arguments[arguments.length - 1].toLowerCase();

            if (currentArg.startsWith("t:") || currentArg.startsWith("time:")) {
                return Arrays.asList("t:1h", "t:3h", "t:6h", "t:12h", "t:1d", "t:1w", "t:1mo");
            }

            if (currentArg.startsWith("r:") || currentArg.startsWith("radius:")) {
                return Arrays.asList("r:10", "r:20", "r:50", "r:#world", "r:10,10,10");
            }

            if (currentArg.startsWith("u:") || currentArg.startsWith("users:")) {
                return Arrays.asList("u:player1", "u:player1,player2");
            }

            if (currentArg.startsWith("i:") || currentArg.startsWith("include:")) {
                return Arrays.asList("i:diamond", "i:stone,dirt", "i:ore");
            }

            if (currentArg.startsWith("e:") || currentArg.startsWith("exclude:")) {
                return Arrays.asList("e:air", "e:stone", "e:dirt");
            }

            return Stream.of("t:3h", "t:6h", "t:12h", "t:1d", "r:10", "u:", "i:", "e:", "#verbose", "#silent")
                .filter(name -> name.contains(currentArg))
                .collect(Collectors.toList());
        }

        return Arrays.asList("t:3h", "t:6h", "t:12h", "t:1d", "r:10", "u:", "i:", "e:", "#verbose", "#silent");
    }

}