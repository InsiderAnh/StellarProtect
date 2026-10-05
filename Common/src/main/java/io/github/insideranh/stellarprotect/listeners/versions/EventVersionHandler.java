package io.github.insideranh.stellarprotect.listeners.versions;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockStateLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.world.BrewingLogEntry;
import io.github.insideranh.stellarprotect.database.entries.world.RaidLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Raid;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EventVersionHandler implements EventLogicHandler {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onPortalCreate(List<Block> blocks) {
        for (Block block : blocks) {
            if (ActionType.BLOCK_PLACE.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;
            LoggerCache.addLog(new PlayerBlockLogEntry(PlayerUtils.getEntityByDirectId("=portal"), block, ActionType.BLOCK_PLACE));
        }
    }

    @Override
    public void onSmithEvent(HumanEntity humanEntity, ItemStack result) {
        if (!(humanEntity instanceof Player)) return;
        Player player = (Player) humanEntity;
        if (ActionType.SMITH.shouldSkipLog(player.getWorld().getName(), result.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        ItemReference itemReference = plugin.getItemsManager().getItemReference(result);
        LoggerCache.addLog(new PlayerItemLogEntry(playerProtect.getPlayerId(), itemReference, player.getLocation(), ActionType.SMITH));
    }

    @Override
    public void onBrewEvent(ItemStack ingredient, ItemStack fuel, List<ItemStack> results) {
        if (!plugin.getConfigManager().isLiquidTracking()) return;
        for (ItemStack result : results) {
            if (result == null || result.getType().name().equals("AIR")) continue;
            BrewingLogEntry entry = new BrewingLogEntry(Bukkit.getPlayerExact(""), ingredient, fuel, result);
            LoggerCache.addLog(entry);
        }
    }

    @Override
    public void onTotemEvent(Entity entity, String hand) {
        if (ActionType.TOTEM.shouldSkipLog(entity.getWorld().getName(), entity.getType().name())) return;
        long longId = PlayerUtils.getPlayerOrEntityId(entity.getType().name());
        LoggerCache.addLog(new io.github.insideranh.stellarprotect.database.entries.entity.EntityResurrectEntry(longId, entity.getLocation(), hand, ActionType.TOTEM));
    }

    @Override
    public void onMount(Entity mount, Entity entity) {
        if (!(mount instanceof Player)) return;
        Player player = (Player) mount;
        if (ActionType.MOUNT.shouldSkipLog(player.getWorld().getName(), entity.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        io.github.insideranh.stellarprotect.database.entries.players.PlayerMountEntry mountEntry =
                new io.github.insideranh.stellarprotect.database.entries.players.PlayerMountEntry(playerProtect.getPlayerId(), player.getLocation(), entity, true);
        LoggerCache.addLog(mountEntry);
    }

    @Override
    public void onDismount(Entity dismounted, Entity entity) {
        if (!(dismounted instanceof Player)) return;
        Player player = (Player) dismounted;
        if (ActionType.MOUNT.shouldSkipLog(player.getWorld().getName(), entity.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        io.github.insideranh.stellarprotect.database.entries.players.PlayerMountEntry mountEntry =
                new io.github.insideranh.stellarprotect.database.entries.players.PlayerMountEntry(playerProtect.getPlayerId(), player.getLocation(), entity, false);
        LoggerCache.addLog(mountEntry);
    }

    @Override
    public void onLeash(HumanEntity humanEntity, Entity entity) {
        if (!(humanEntity instanceof Player)) return;
        Player player = (Player) humanEntity;
        if (ActionType.LEASH.shouldSkipLog(player.getWorld().getName(), entity.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        io.github.insideranh.stellarprotect.database.entries.players.PlayerLeashEntry mountEntry =
                new io.github.insideranh.stellarprotect.database.entries.players.PlayerLeashEntry(playerProtect.getPlayerId(), player.getLocation(), entity, true);
        LoggerCache.addLog(mountEntry);
    }

    @Override
    public void onUnleash(HumanEntity humanEntity, Entity entity) {
        if (!(humanEntity instanceof Player)) return;
        Player player = (Player) humanEntity;
        if (ActionType.LEASH.shouldSkipLog(player.getWorld().getName(), entity.getType().name())) return;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;
        io.github.insideranh.stellarprotect.database.entries.players.PlayerLeashEntry mountEntry =
                new io.github.insideranh.stellarprotect.database.entries.players.PlayerLeashEntry(playerProtect.getPlayerId(), player.getLocation(), entity, false);
        LoggerCache.addLog(mountEntry);
    }

    @Override
    public void onRaidTrigger(Player player, Raid raid) {
        if (player == null || raid == null) return;
        long playerId = io.github.insideranh.stellarprotect.utils.PlayerUtils.getPlayerOrEntityId(player.getName());
        LoggerCache.addLog(new RaidLogEntry(playerId, raid.getLocation(), RaidLogEntry.RaidPhase.TRIGGER));
    }

    @Override
    public void onRaidSpawn(Raid raid) {
        if (raid == null) return;
        LoggerCache.addLog(new RaidLogEntry(-2L, raid.getLocation(), RaidLogEntry.RaidPhase.SPAWN));
    }

    @Override
    public void onRaidFinish(Raid raid) {
        if (raid == null) return;
        LoggerCache.addLog(new RaidLogEntry(-2L, raid.getLocation(), RaidLogEntry.RaidPhase.FINISH));
    }

    @Override
    public void onBlockFade(BlockState oldState, BlockState newState) {
        if (oldState == null || newState == null) return;
        if (ActionType.BLOCK_FADE.shouldSkipLog(oldState.getWorld().getName(), oldState.getType().name())) return;
        if (ActionType.BLOCK_FADE.shouldSkipLog(newState.getWorld().getName(), newState.getType().name())) return;
        LoggerCache.addLog(new PlayerBlockStateLogEntry(PlayerUtils.getEntityByDirectId("=natural"), oldState, newState, ActionType.BLOCK_FADE));
    }

    @Override
    public void onBlockBurn(Block block) {
        if (block == null) return;
        if (ActionType.BLOCK_BURN.shouldSkipLog(block.getWorld().getName(), block.getType().name())) return;
        Long cachedPlayerId = io.github.insideranh.stellarprotect.cache.BlockSourceCache.getPlayerId(block.getLocation());
        long playerId = cachedPlayerId != null ? cachedPlayerId : PlayerUtils.getEntityByDirectId("=fire");
        LoggerCache.addLog(new PlayerBlockLogEntry(playerId, block, ActionType.BLOCK_BURN));
    }

    @Override
    public void onBlockForm(BlockState oldState, BlockState newState) {
        if (oldState == null || newState == null) return;
        if (ActionType.BLOCK_FORM.shouldSkipLog(newState.getWorld().getName(), newState.getType().name())) return;
        LoggerCache.addLog(new PlayerBlockStateLogEntry(PlayerUtils.getEntityByDirectId("=natural"), oldState, newState, ActionType.BLOCK_FORM));
    }

    @Override
    public void onDispense(Block block, ItemStack item) {
        if (block == null || item == null) return;
        if (ActionType.BLOCK_DISPENSE.shouldSkipLog(block.getWorld().getName(), item.getType().name())) return;
        LoggerCache.addLog(new PlayerBlockLogEntry(PlayerUtils.getEntityByDirectId("=dispenser"), block, ActionType.BLOCK_DISPENSE));
    }

}