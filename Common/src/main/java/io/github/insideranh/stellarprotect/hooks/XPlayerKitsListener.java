package io.github.insideranh.stellarprotect.hooks;

import io.github.InsiderAnh.xPlayerKits.api.events.ClaimXKitEvent;
import io.github.InsiderAnh.xPlayerKits.api.events.GiveXKitEvent;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerXKitEventLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class XPlayerKitsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaim(ClaimXKitEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        String kitName = event.getKitName();
        if (ActionType.X_KIT_EVENT.shouldSkipLog(player.getWorld().getName(), kitName)) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerXKitEventLogEntry(playerProtect.getPlayerId(), player.getLocation(), (byte) 0, kitName));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGive(GiveXKitEvent event) {
        if (event.isCancelled()) return;

        Player receiver = event.getReceiver();
        String kitName = event.getKitName();
        if (ActionType.X_KIT_EVENT.shouldSkipLog(receiver.getWorld().getName(), kitName)) return;

        CommandSender sender = event.getGiver();
        long playerId = PlayerUtils.getPlayerOrConsoleId(sender);

        LoggerCache.addLog(new PlayerXKitEventLogEntry(playerId, receiver.getLocation(), (byte) 1, kitName));
    }

}