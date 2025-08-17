package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerTameEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (block.getType().equals(Material.AIR) || ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name()))
            return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_BREAK));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), "=decay")) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(PlayerUtils.getEntityByDirectId("=decay"), block, ActionType.BLOCK_BREAK));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerBlockLogEntry(playerProtect.getPlayerId(), block, ActionType.BLOCK_PLACE));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Block block = event.getBlock();
        if (ActionType.FURNACE_EXTRACT.shouldSkipLog(block.getWorld().getName(), event.getItemType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(event.getPlayer());
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(new ItemStack(event.getItemType(), event.getItemAmount()));

        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, block.getLocation(), ActionType.FURNACE_EXTRACT));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;

        Player player = (Player) event.getOwner();

        if (!(event.getEntity() instanceof Animals)) return;
        Animals animal = (Animals) event.getEntity();
        if (ActionType.TAME.shouldSkipLog(animal.getWorld().getName(), animal.getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerTameEntry(playerProtect.getPlayerId(), animal));
    }

}