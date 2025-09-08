package io.github.insideranh.stellarprotect.nms.v1_9_R4.listeners;

import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.Arrays;

public class BlockListener_v1_9_R4 implements Listener {

    private final EventLogicHandler eventLogicHandler;

    public BlockListener_v1_9_R4(EventLogicHandler eventLogicHandler) {
        this.eventLogicHandler = eventLogicHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        this.eventLogicHandler.onPortalCreate(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewEvent(BrewEvent event) {
        this.eventLogicHandler.onBrewEvent(event.getContents().getIngredient(), event.getContents().getFuel(), Arrays.asList(event.getContents().getContents()));
    }

    @EventHandler
    public void onMount(EntityMountEvent event) {
        this.eventLogicHandler.onMount(event.getMount(), event.getEntity());
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        this.eventLogicHandler.onDismount(event.getDismounted(), event.getEntity());
    }

}