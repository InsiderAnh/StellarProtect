package io.github.insideranh.stellarprotect.nms.v1_21_R9.listeners;

import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import org.bukkit.Raid;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class BlockListener_v1_21_R9 implements Listener {

    private final EventLogicHandler eventLogicHandler;

    public BlockListener_v1_21_R9(EventLogicHandler eventLogicHandler) {
        this.eventLogicHandler = eventLogicHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        this.eventLogicHandler.onPortalCreate(event.getBlocks().stream().map(BlockState::getBlock).collect(Collectors.toCollection(ArrayList::new)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmithEvent(SmithItemEvent event) {
        this.eventLogicHandler.onSmithEvent(event.getWhoClicked(), event.getCurrentItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewEvent(BrewEvent event) {
        this.eventLogicHandler.onBrewEvent(event.getContents().getIngredient(), event.getContents().getFuel(), event.getResults());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemEvent(EntityResurrectEvent event) {
        this.eventLogicHandler.onTotemEvent(event.getEntity(), event.getHand() == null ? "" : event.getHand().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidTriggerEvent event) {
        Player player = event.getPlayer();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidStopEvent event) {
        Raid raid = event.getRaid();

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidSpawnWaveEvent event) {
        Raid raid = event.getRaid();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaid(RaidFinishEvent event) {
        Raid raid = event.getRaid();
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