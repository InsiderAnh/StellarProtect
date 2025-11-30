package io.github.insideranh.stellarprotect.nms.v1_21.worldedit;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

public class WorldEditBlockState implements BlockState {

    protected Location location;
    protected Material material;
    protected BlockData blockData;

    public WorldEditBlockState(Location loc) {
        location = loc;
    }

    public WorldEditBlockState(Location loc, Material type, BlockData data) {
        location = loc;
        material = type;
        blockData = data;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {

    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return null;
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {

    }

    @Override
    public Block getBlock() {
        return null;
    }

    @Override
    public MaterialData getData() {
        return null;
    }

    @Override
    @Deprecated
    public void setData(MaterialData data) {
    }

    @Override
    public BlockData getBlockData() {
        return blockData;
    }

    @Override
    public void setBlockData(BlockData data) {
        this.blockData = data;
    }

    @Override
    public Material getType() {
        return material;
    }

    @Override
    public void setType(Material type) {
        this.material = type;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public World getWorld() {
        return Objects.requireNonNull(location.getWorld());
    }

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public Location getLocation(Location loc) {
        if (loc != null) {
            loc.setWorld(location.getWorld());
            loc.setX(location.getX());
            loc.setY(location.getY());
            loc.setZ(location.getZ());
            loc.setYaw(location.getYaw());
            loc.setPitch(location.getPitch());
        }
        return loc;
    }

    @Override
    public Chunk getChunk() {
        return location.getChunk();
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public boolean update(boolean force) {
        return false;
    }

    @Override
    public boolean update(boolean force, boolean applyPhysics) {
        return false;
    }

    @Override
    @Deprecated
    public byte getRawData() {
        return 0;
    }

    @Override
    @Deprecated
    public void setRawData(byte data) {
    }

    @Override
    public boolean isPlaced() {
        return false;
    }

    @Override
    public BlockState copy() {
        return new WorldEditBlockState(location, material, blockData);
    }

    @Override
    public BlockState copy(Location location) {
        return new WorldEditBlockState(location, material, blockData);
    }

}