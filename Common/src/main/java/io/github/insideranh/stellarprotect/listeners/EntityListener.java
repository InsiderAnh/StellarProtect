package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerHangingEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerKillLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerShootEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

public class EntityListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() == null) return;
        if (ActionType.KILL_ENTITY.shouldSkipLog(entity.getWorld().getName(), entity.getType().name())) return;

        Player killer = entity.getKiller();

        PlayerProtect playerProtect = PlayerProtect.getPlayer(killer);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerKillLogEntry(playerProtect.getPlayerId(), entity, ActionType.KILL_ENTITY));
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        Projectile projectile = (Projectile) event.getDamager();

        if (ActionType.SHOOT.shouldSkipLog(projectile.getWorld().getName(), projectile.getType().name())) return;
        if (!(projectile.getShooter() instanceof Player)) return;

        Player player = (Player) projectile.getShooter();

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerShootEntry shootEntry = new PlayerShootEntry(playerProtect.getPlayerId(), player.getLocation(), projectile, event.getEntity(), true);
        LoggerCache.addLog(shootEntry);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return;

        Player player = (Player) event.getRemover();
        if (ActionType.HANGING.shouldSkipLog(player.getWorld().getName(), event.getEntity().getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerHangingEntry hangingEntry = new PlayerHangingEntry(playerProtect.getPlayerId(), player.getLocation(), event.getEntity());

        LoggerCache.addLog(hangingEntry);
    }


}