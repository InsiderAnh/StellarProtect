package io.github.insideranh.stellarprotect.nms.v1_21_R7.listeners;

import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class BlockListener_v1_21_R7 implements Listener {

    private final EventLogicHandler eventLogicHandler;

    public BlockListener_v1_21_R7(EventLogicHandler eventLogicHandler) {
        this.eventLogicHandler = eventLogicHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        this.eventLogicHandler.onPortalCreate(event.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toCollection(ArrayList::new)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmithEvent(SmithItemEvent event) {
        this.eventLogicHandler.onSmithEvent(event.getWhoClicked(), event.getInventory().getResult());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewEvent(BrewEvent event) {
        this.eventLogicHandler.onBrewEvent(event.getContents().getIngredient(), event.getContents().getFuel(), event.getResults());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemEvent(EntityResurrectEvent event) {
        this.eventLogicHandler.onTotemEvent(event.getEntity(), event.getHand() == null ? "" : event.getHand().name());
    }

}