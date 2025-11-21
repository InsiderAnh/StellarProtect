package io.github.insideranh.stellarprotect.api;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.InspectSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerChatEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class StellarProtectAPI {

    private static final StellarProtect plugin = StellarProtect.getInstance();

    public static void logPlace(Player player, Block block, boolean ignoreSkip) {
        if (ignoreSkip || ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name()))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_PLACE));
    }

    public static void logBreak(Player player, Block block, boolean ignoreSkip) {
        if (ignoreSkip || ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name()))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_BREAK));
    }

    public static void logChat(Player player, String message, boolean ignoreSkip) {
        if (ignoreSkip || ActionType.CHAT.shouldSkipLog(player.getWorld().getName(), message)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerChatEntry(playerProtect.getPlayerId(), player, message));
    }

    public static void logCommand(Player player, String command, boolean ignoreSkip) {
        if (ignoreSkip || ActionType.COMMAND.shouldSkipLog(player.getWorld().getName(), command)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerCommandEntry(playerProtect.getPlayerId(), player, command));
    }

    public static void inspect(Player player, PlayerInteractEvent event, Block block) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        Action action = event.getAction();

        Location blockLocation = block.getLocation();
        playerProtect.setInspectSession(new InspectSession(blockLocation, 0, 10, WorldUtils.isValidChestBlock(block.getType())));

        if (WorldUtils.isValidChestBlock(block.getType()) && action.equals(Action.RIGHT_CLICK_BLOCK)) {
            plugin.getInspectHandler().handleChestInspection(player, blockLocation, 1, 0, 10);
        } else {
            if (event.hasItem() && event.getItem().getType().isBlock()) {
                Block blockFace = block.getRelative(event.getBlockFace());
                plugin.getInspectHandler().handleBlockInspection(player, blockFace.getLocation(), 1, 0, 10);
            } else {
                plugin.getInspectHandler().handleBlockInspection(player, blockLocation, 1, 0, 10);
            }
        }
    }

    public static void inspectBlock(Player player, Location location) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        playerProtect.setInspectSession(new InspectSession(location, 0, 10, false));
        plugin.getInspectHandler().handleBlockInspection(player, location, 1, 0, 10);
    }

    public static void inspectChest(Player player, Location location) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        playerProtect.setInspectSession(new InspectSession(location, 0, 10, true));
        plugin.getInspectHandler().handleChestInspection(player, location, 1, 0, 10);
    }

    public static void inspectPlacedBlock(Player player, Block block, BlockFace blockFace) {
        plugin.getInspectHandler().handleBlockInspection(player, block.getRelative(blockFace).getLocation(), 1, 0, 10);
    }

}