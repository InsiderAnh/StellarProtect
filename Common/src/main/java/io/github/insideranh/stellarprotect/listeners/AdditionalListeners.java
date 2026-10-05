package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class AdditionalListeners implements Listener {

    private static final List<String> SCULK_BLOCKS = Arrays.asList(
            "SCULK", "SCULK_VEIN", "SCULK_CATALYST", "SCULK_SENSOR", "SCULK_SHRIEKER"
    );
    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        ItemStack item = event.getItem();
        if (block == null || item == null) return;

        plugin.getEventLogicHandler().onDispense(block, item);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getInventory().getResult();
        if (player == null || result == null) return;

        plugin.getProtectNMS().sendActionTitle(player,
                plugin.getLangManager().get("messages.smith.upgrade"),
                plugin.getLangManager().get("messages.tooltips.smith"),
                "/sp view smith " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ(),
                text -> text
                        .replace("<time>", "now")
                        .replace("<player>", player.getName())
                        .replace("<data>", result.getType().name())
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        ItemStack ingredient = event.getContents().getIngredient();
        ItemStack fuel = null;
        if (event.getContents().getFuel() != null) {
            fuel = event.getContents().getFuel();
        }
        plugin.getEventLogicHandler().onBrewEvent(ingredient, fuel, event.getResults());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().forEach(block -> {
            if (SCULK_BLOCKS.contains(block.getType().name())) {
                Long playerId = BlockSourceCache.getPlayerId(block.getLocation());
                if (playerId != null && !ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
                    LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().forEach(block -> {
            if (SCULK_BLOCKS.contains(block.getType().name())) {
                Long playerId = BlockSourceCache.getPlayerId(block.getLocation());
                if (playerId != null && !ActionType.BLOCK_BREAK.shouldSkipLog(block.getWorld().getName(), block.getType().name())) {
                    LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BREAK));
                }
            }
        });
    }

}