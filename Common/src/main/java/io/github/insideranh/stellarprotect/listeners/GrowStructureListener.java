package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.Nullable;

public class GrowStructureListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent event) {
        if (ActionType.TREE_GROW.shouldSkipLog(event.getWorld().getName(), event.getSpecies().name())) return;

        event.getBlocks().forEach(block -> LoggerCache.addLog(new PlayerBlockLogEntry(getPlayerId(event.getPlayer()), block, ActionType.TREE_GROW)));
    }

    public long getPlayerId(@Nullable Player player) {
        if (player == null) return -2L;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        return playerProtect == null ? -2L : playerProtect.getPlayerId();
    }

}