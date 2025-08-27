package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerSessionEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getJoinExecutor().execute(() -> {
            PlayerProtect playerProtect = plugin.getProtectDatabase().loadOrCreatePlayer(player);
            if (playerProtect != null) {
                playerProtect.create();
                playerProtect.setLoginTime(System.currentTimeMillis());

                LoggerCache.addLog(new PlayerSessionEntry(playerProtect.getPlayerId(), player.getLocation(), (byte) 1, 0));

                PlayerCache.cacheName(playerProtect.getPlayerId(), player.getName());
            }
        });

        if (!plugin.getConfigManager().isCheckUpdates()) return;
        if (!player.hasPermission("stellarprotect.admin") || plugin.getUpdateChecker() == null) return;

        plugin.getUpdateChecker().sendUpdateMessage(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.removePlayer(player);
        if (playerProtect == null) return;

        PickUpDropListener.forceFlushCurrentGroup(playerProtect, true);

        long time = (System.currentTimeMillis() - playerProtect.getLoginTime()) / 1000L;
        if (time > 0) {
            LoggerCache.addLog(new PlayerSessionEntry(playerProtect.getPlayerId(), player.getLocation(), (byte) 0, time));
        }

        PlayerCache.removeCacheName(playerProtect.getPlayerId());
    }

}