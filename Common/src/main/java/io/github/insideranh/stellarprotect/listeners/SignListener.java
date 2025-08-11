package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerSignChangeEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onSign(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (ActionType.SIGN_CHANGE.shouldSkipLog(player.getWorld().getName(), "SIGN")) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        Block block = event.getBlock();
        LoggerCache.addLog(new PlayerSignChangeEntry(playerProtect.getPlayerId(), block.getLocation(), event.getLines(), ActionType.SIGN_CHANGE));
    }

}