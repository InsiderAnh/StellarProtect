package io.github.insideranh.stellarprotect.nms.v1_21.worldedit;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import io.github.insideranh.stellarprotect.nms.v1_21.WorldEditHandler_v1_21;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

public class StellarLogger extends AbstractDelegateExtent {

    private final Actor eventActor;
    private final World eventWorld;
    private final Extent eventExtent;

    protected StellarLogger(Actor actor, World world, Extent extent) {
        super(extent);
        this.eventActor = actor;
        this.eventWorld = world;
        this.eventExtent = extent;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        org.bukkit.World world = BukkitAdapter.adapt(eventWorld);

        BlockState oldBlock = eventExtent.getBlock(position);
        Material oldType = BukkitAdapter.adapt(oldBlock.getBlockType());
        Location location = new Location(world, position.x(), position.y(), position.z());

        BaseBlock baseBlock = WorldEditLogger.getBaseBlock(eventExtent, position, location, oldType);

        ItemStack[] containerData = getContainerContents(location, oldType);

        if (WorldEditHandler_v1_21.isFawe()) {
            if (eventExtent.setBlock(position.x(), position.y(), position.z(), block)) {
                WorldEditLogger.postProcess(eventExtent, eventActor, position, location, block, baseBlock, oldType, oldBlock, containerData);
                return true;
            }
        } else {
            if (eventExtent.setBlock(position, block)) {
                WorldEditLogger.postProcess(eventExtent, eventActor, position, location, block, baseBlock, oldType, oldBlock, containerData);
                return true;
            }
        }

        return false;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        return this.setBlock(BlockVector3.at(x, y, z), block);
    }

    @Override
    public int replaceBlocks(final Region region, final Mask mask, final Pattern pattern) throws MaxChangedBlocksException {
        org.bukkit.World world = BukkitAdapter.adapt(eventWorld);

        processPatternToBlocks(world, region, pattern);

        return eventExtent.replaceBlocks(region, mask, pattern);
    }

    @Override
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        org.bukkit.World world = BukkitAdapter.adapt(eventWorld);

        processPatternToBlocks(world, region, pattern);

        return eventExtent.setBlocks(region, pattern);
    }

    private void processPatternToBlocks(org.bukkit.World world, Region region, Pattern pattern) {
        try {
            for (BlockVector3 position : region) {
                BlockState oldBlock = eventExtent.getBlock(position);
                Material oldType = BukkitAdapter.adapt(oldBlock.getBlockType());
                Location location = new Location(world, position.x(), position.y(), position.z());

                BaseBlock baseBlock = WorldEditLogger.getBaseBlock(eventExtent, position, location, oldType);

                ItemStack[] containerData = getContainerContents(location, oldType);

                BlockStateHolder<?> newBlock = pattern.applyBlock(position);
                WorldEditLogger.postProcess(eventExtent, eventActor, position, location, newBlock, baseBlock, oldType, oldBlock, containerData);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[StellarProtect] Error processing pattern to blocks: " + e.getMessage());
        }
    }

    private ItemStack[] getContainerContents(Location location, Material material) {
        if (!isContainer(material)) return null;

        try {
            org.bukkit.block.BlockState state = location.getBlock().getState();
            if (state instanceof Container) {
                Container container = (Container) state;
                return container.getInventory().getContents();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean isContainer(Material material) {
        String name = material.name();
        return name.contains("CHEST") ||
            name.contains("BARREL") ||
            name.contains("SHULKER_BOX") ||
            name.contains("COPPER_CHEST") ||
            material == Material.HOPPER ||
            material == Material.DROPPER ||
            material == Material.DISPENSER ||
            material == Material.FURNACE ||
            material == Material.BLAST_FURNACE ||
            material == Material.SMOKER;
    }

}