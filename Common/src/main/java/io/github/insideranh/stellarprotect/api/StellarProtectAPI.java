package io.github.insideranh.stellarprotect.api;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerChatEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

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

}