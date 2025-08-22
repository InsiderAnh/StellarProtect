package io.github.insideranh.stellarprotect.listeners;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerXPEntry;
import io.github.insideranh.stellarprotect.database.entries.players.*;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.DeathCause;
import io.github.insideranh.stellarprotect.enums.TeleportType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.listeners.handlers.GenericHandler;
import io.github.insideranh.stellarprotect.listeners.handlers.Handlers;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerLogListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ActionType.DEATH.shouldSkipLog(event.getEntity().getWorld().getName(), event.getEntity().getType().name()))
            return;

        Player player = event.getEntity();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        DeathCause cause = DeathCause.getById(getCause(player));

        PlayerDeathEntry deathEntry = new PlayerDeathEntry(playerProtect.getPlayerId(), player.getLocation(), cause.getId());

        LoggerCache.addLog(deathEntry);
        PlayerCache.checkPattern(deathEntry);
    }

    @EventHandler
    public void onLaunchProjectile(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        if (ActionType.SHOOT.shouldSkipLog(player.getWorld().getName(), event.getEntity().getType().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerShootEntry shootEntry = new PlayerShootEntry(playerProtect.getPlayerId(), player.getLocation(), event.getEntity(), false);

        LoggerCache.addLog(shootEntry);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (ActionType.TELEPORT.shouldSkipLog(player.getWorld().getName(), "other")) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerTeleportLogEntry locationLogEntry = new PlayerTeleportLogEntry(playerProtect.getPlayerId(), player.getLocation(), TeleportType.getByName(event.getCause().name()));
        LoggerCache.addLog(locationLogEntry);
    }

    @EventHandler
    public void onPlayerGameMode(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (ActionType.GAME_MODE.shouldSkipLog(player.getWorld().getName(), event.getNewGameMode().name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        PlayerGameModeLogEntry locationLogEntry = new PlayerGameModeLogEntry(playerProtect.getPlayerId(), player.getLocation(), player.getGameMode().ordinal(), event.getNewGameMode().ordinal());
        LoggerCache.addLog(locationLogEntry);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItem().getType();

        if (ActionType.CONSUME.shouldSkipLog(player.getWorld().getName(), material.name())) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemReference itemReference = plugin.getItemsManager().getItemReference(event.getItem());

        PlayerItemLogEntry itemLogEntry = new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, player.getLocation(), ActionType.CONSUME);
        LoggerCache.addLog(itemLogEntry);
    }

    @EventHandler
    public void onXP(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerXPEntry(playerProtect.getPlayerId(), player.getLocation(), event.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Animals)) return;

        Animals animal = (Animals) event.getRightClicked();
        if (!animal.canBreed()) return;
        if (ActionType.BREED.shouldSkipLog(animal.getWorld().getName(), animal.getType().name())) return;

        Player player = event.getPlayer();

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        LoggerCache.addLog(new PlayerBreedEntry(playerProtect.getPlayerId(), animal));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        if (playerProtect.getNextUse() > System.currentTimeMillis()) {
            return;
        }
        playerProtect.setNextUse(System.currentTimeMillis() + 300L);

        Block block = event.getClickedBlock();

        ItemStack item = event.getItem();
        String blockType = block.getType().name();

        GenericHandler genericHandler = Handlers.canHandle(block, blockType, item);
        if (genericHandler != null) {
            if (ActionType.INTERACT.shouldSkipLog(block.getWorld().getName(), blockType)) return;

            genericHandler.handle(player, playerProtect.getPlayerId(), block, item);
        }
    }

    public String getCause(Player player) {
        EntityDamageEvent event = player.getLastDamageCause();

        if (event == null) return "UNKNOWN";
        if (event.getCause() == null) return "UNKNOWN";

        return event.getCause().name();
    }

}