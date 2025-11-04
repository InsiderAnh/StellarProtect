package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.callback.CallbackBucket;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class BucketListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_EMPTY.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        CallbackBucket<Block, String, Material> data = plugin.getProtectNMS().getBucketData(event.getBlockClicked(), event.getBlockFace(), event.getBucket());

        Block liquidBlock = data.getBlock();
        BlockSourceCache.registerBlockSource(liquidBlock.getLocation(), playerProtect.getPlayerId());

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), liquidBlock, ActionType.BUCKET_EMPTY));
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (ActionType.BUCKET_FILL.shouldSkipLog(player.getWorld().getName(), event.getBucket().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        Block liquidBlock = event.getBlockClicked().getRelative(event.getBlockFace());

        BlockSourceCache.removeBlockSource(liquidBlock.getLocation());

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), liquidBlock, ActionType.BUCKET_FILL));
    }

}