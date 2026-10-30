package io.github.insideranh.stellarprotect.restore;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.enums.ExtraDataType;
import io.github.insideranh.stellarprotect.utils.NbtUtils;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class BlockRestore {

    protected final String data;
    protected final byte extraType;
    protected final String extraData;
    protected final boolean isPlace;
    protected final String oldData;
    protected final String blockEntityNbt;
    protected final String oldBlockEntityNbt;

    public BlockRestore(String data) {
        this(data, (byte) 0, null, false, null, null, null);
    }

    public BlockRestore(String data, byte extraType, String extraData) {
        this(data, extraType, extraData, false, null, null, null);
    }

    public BlockRestore(String data, byte extraType, String extraData, boolean isPlace) {
        this(data, extraType, extraData, isPlace, null, null, null);
    }

    public BlockRestore(String data, byte extraType, String extraData, boolean isPlace, String oldData, String blockEntityNbt, String oldBlockEntityNbt) {
        this.data = data;
        this.extraType = extraType;
        this.extraData = extraData;
        this.isPlace = isPlace;
        this.oldData = oldData;
        this.blockEntityNbt = blockEntityNbt;
        this.oldBlockEntityNbt = oldBlockEntityNbt;
    }

    private static org.bukkit.block.data.BlockData createBlockDataSafe(String s) {
        if (s == null || s.trim().isEmpty()) {
            return Material.AIR.createBlockData();
        }
        try {
            return Bukkit.createBlockData(s);
        } catch (Exception e) {
            return Material.AIR.createBlockData();
        }
    }

    public static BlockRestore fromData(String data) {
        return new BlockRestore(data);
    }

    public static BlockRestore fromData(String data, byte extraType, String extraData) {
        return new BlockRestore(data, extraType, extraData);
    }

    public static BlockRestore fromData(String data, byte extraType, String extraData, boolean isPlace) {
        return new BlockRestore(data, extraType, extraData, isPlace);
    }

    public static BlockRestore fromData(String data, byte extraType, String extraData, boolean isPlace, String oldData, String blockEntityNbt, String oldBlockEntityNbt) {
        return new BlockRestore(data, extraType, extraData, isPlace, oldData, blockEntityNbt, oldBlockEntityNbt);
    }

    public void reset(Gson gson, Location location) {
        Block block = location.getBlock();
        try {
            if (isPlace) {
                applyOldBlockData(block);
            } else {
                applyNewBlockData(block);
            }
            applyContainerForReset(block);
            applyBlockNbt(block);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[StellarProtect] Failed to restore block at " + location + ": " + e.getMessage());
        }
    }

    public void undoPlace(Gson gson, Location location) {
        Block block = location.getBlock();
        try {
            applyNewBlockData(block);
            applyContainerFromJson(block);
            applyNewBlockNbt(block);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[StellarProtect] Failed to undo place at " + location + ": " + e.getMessage());
        }
    }

    public void preview(Player player, Gson gson, Location location) {
        try {
            BlockData blockData = createBlockDataSafe(data);
            player.sendBlockChange(location, blockData);
        } catch (Exception ignored) {
        }
    }

    public void previewRemove(Player player, Location location) {
        try {
            player.sendBlockChange(location, Material.AIR.createBlockData());
        } catch (Exception ignored) {
        }
    }

    public void remove(Location location) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
    }

    public boolean hasContainerContent() {
        return extraType == ExtraDataType.INVENTORY_CONTENT.getId() && extraData != null && !extraData.isEmpty();
    }

    public boolean isPlaceAction() {
        return isPlace;
    }

    public String getData() {
        return data;
    }

    public byte getExtraType() {
        return extraType;
    }

    public String getExtraData() {
        return extraData;
    }

    public String getOldData() {
        return oldData;
    }

    public String getBlockEntityNbt() {
        return blockEntityNbt;
    }

    public String getOldBlockEntityNbt() {
        return oldBlockEntityNbt;
    }

    protected void applyNewBlockData(Block block) {
        if (data == null || data.trim().isEmpty()) {
            block.setType(Material.AIR);
            return;
        }
        try {
            BlockData blockData = createBlockDataSafe(data);
            block.setBlockData(blockData, false);
        } catch (Exception e) {
            block.setType(Material.AIR);
        }
    }

    protected void applyOldBlockData(Block block) {
        if (oldData != null && !oldData.isEmpty()) {
            try {
                BlockData blockData = createBlockDataSafe(oldData);
                block.setBlockData(blockData, false);
                return;
            } catch (Exception ignored) {
            }
        }
        block.setType(Material.AIR);
    }

    protected void applyContainerFromJson(Block block) {
        if (!hasContainerContent()) return;
        try {
            BlockState state = block.getState();
            if (state instanceof InventoryHolder) {
                JsonObject obj = new JsonParser().parse(extraData).getAsJsonObject();
                SerializerUtils.setInventoryContent(((InventoryHolder) state).getInventory(), obj);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyContainerForReset(Block block) {
        if (!isPlace) {
            applyContainerFromJson(block);
        }
    }

    protected void applyBlockNbt(Block block) {
        String nbt = isPlace ? oldBlockEntityNbt : blockEntityNbt;
        if (nbt == null || nbt.isEmpty()) return;
        BlockState state = block.getState();
        if (state instanceof TileState) {
            NbtUtils.applyBlockEntity((TileState) state, nbt);
        }
    }

    private void applyNewBlockNbt(Block block) {
        if (blockEntityNbt == null || blockEntityNbt.isEmpty()) return;
        BlockState state = block.getState();
        if (state instanceof TileState) {
            NbtUtils.applyBlockEntity((TileState) state, blockEntityNbt);
        }
    }

}
