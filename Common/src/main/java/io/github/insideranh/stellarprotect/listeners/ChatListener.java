package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerChatEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (ActionType.CHAT.shouldSkipLogStart(player.getWorld().getName(), message)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerChatEntry(playerProtect.getPlayerId(), player, message));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (ActionType.COMMAND.shouldSkipLogStart(player.getWorld().getName(), message)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerCommandEntry commandEntry = new PlayerCommandEntry(playerProtect.getPlayerId(), player, message);

        LoggerCache.addLog(commandEntry);
        PlayerCache.checkPattern(commandEntry);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getSender() instanceof Player) return;
        String message = event.getCommand();

        if (ActionType.COMMAND.shouldSkipLogStart(null, message)) return;

        LoggerCache.addLog(new PlayerCommandEntry(-1, message));
    }

}