package io.github.insideranh.stellarprotect.listeners.versions;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.entity.EntityResurrectEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerBlockLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.block.Block;
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
        /*plugin.getLogger().info("Brew event ingredient: " + ingredient.getType().name() + " fuel: " + fuel.getType().name());
        for (ItemStack result : results) {
            plugin.getLogger().info("Result: " + result.getType().name());
        }*/
    }

    @Override
    public void onTotemEvent(Entity entity, String hand) {
        if (ActionType.TOTEM.shouldSkipLog(entity.getWorld().getName(), entity.getType().name())) return;

        long playerId = PlayerUtils.getPlayerOrEntityId(entity.getType().name());
        LoggerCache.addLog(new EntityResurrectEntry(playerId, entity.getLocation(), hand, ActionType.TOTEM));
    }

}