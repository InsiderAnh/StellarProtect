package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.ExtraDataType;
import io.github.insideranh.stellarprotect.managers.BlocksManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.sql.ResultSet;

@Getter
public class PlayerBlockLogEntry extends LogEntry {

    private static final BlocksManager blocksManager = StellarProtect.getInstance().getBlocksManager();
    private final Integer blockId;
    private Integer oldBlockId;
    private String nexoBlockId;
    private byte extraType;
    private String extraData;
    private String oldBlockData;
    private String blockEntityNbt;
    private String oldBlockEntityNbt;

    @SneakyThrows
    public PlayerBlockLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.blockId = getBlockId(jsonObject);
        this.oldBlockId = getOldBlockId(jsonObject);
        if (jsonObject.has("nbId")) {
            this.nexoBlockId = jsonObject.get("nbId").getAsString();
        }
        if (jsonObject.has("xt")) {
            this.extraType = jsonObject.get("xt").getAsByte();
        }
        if (jsonObject.has("xd")) {
            this.extraData = jsonObject.get("xd").getAsString();
        }
        if (jsonObject.has("od")) {
            this.oldBlockData = jsonObject.get("od").getAsString();
        }
        if (jsonObject.has("nbt")) {
            this.blockEntityNbt = jsonObject.get("nbt").getAsString();
        }
        if (jsonObject.has("onbt")) {
            this.oldBlockEntityNbt = jsonObject.get("onbt").getAsString();
        }
    }

    public PlayerBlockLogEntry(long playerId, BlockState oldBlockState, BlockState newBlockState, ActionType actionType) {
        super(playerId, actionType.getId(), newBlockState.getLocation(), System.currentTimeMillis());
        BlockTemplate oldBlockTemplate = blocksManager.getBlockTemplate(oldBlockState);
        this.oldBlockId = oldBlockTemplate.getId();
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(newBlockState);
        this.blockId = blockTemplate.getId();
        setBlockId(this.blockId);
        setOldBlockId(this.oldBlockId);
        captureNbt(oldBlockState, newBlockState);
    }

    public PlayerBlockLogEntry(long playerId, BlockState blockState, ActionType actionType) {
        super(playerId, actionType.getId(), blockState.getLocation(), System.currentTimeMillis());
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(blockState);
        this.blockId = blockTemplate.getId();
        setBlockId(this.blockId);
        captureNbt(null, blockState);
        if (actionType.getId() != ActionType.BLOCK_PLACE.getId() && actionType.getId() != ActionType.BLOCK_BREAK.getId())
            return;
        if (blockState instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) blockState).getInventory();
            JsonObject jsonObject = StellarProtect.getInstance().getChestTransactionTracker().getInventoryContent(inventory);
            this.extraType = ExtraDataType.INVENTORY_CONTENT.getId();
            this.extraData = jsonObject.toString();
        }
    }

    public PlayerBlockLogEntry(long playerId, Location location, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(block);
        this.blockId = blockTemplate.getId();
        setBlockId(this.blockId);
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(block);
        this.blockId = blockTemplate.getId();
        setBlockId(this.blockId);
        captureNbt(null, block.getState());
        if (actionType.getId() != ActionType.BLOCK_PLACE.getId() && actionType.getId() != ActionType.BLOCK_BREAK.getId())
            return;
        if (block.getState() instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) block.getState()).getInventory();
            JsonObject jsonObject = StellarProtect.getInstance().getChestTransactionTracker().getInventoryContent(inventory);
            this.extraType = ExtraDataType.INVENTORY_CONTENT.getId();
            this.extraData = jsonObject.toString();
        }
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType, String nexoBlockId) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(block);
        this.blockId = blockTemplate.getId();
        setBlockId(this.blockId);
        this.nexoBlockId = "nexo:" + nexoBlockId;
    }

    private void captureNbt(BlockState oldState, BlockState newState) {
        try {
            if (newState != null && newState.getBlock().getState() instanceof org.bukkit.block.TileState) {
                org.bukkit.block.TileState ts = (org.bukkit.block.TileState) newState.getBlock().getState();
                io.github.insideranh.stellarprotect.utils.NbtUtils.writeBlockEntity(ts, json -> this.blockEntityNbt = json);
            }
            if (oldState != null && oldState.getBlock().getState() instanceof org.bukkit.block.TileState) {
                org.bukkit.block.TileState ts = (org.bukkit.block.TileState) oldState.getBlock().getState();
                io.github.insideranh.stellarprotect.utils.NbtUtils.writeBlockEntity(ts, json -> this.oldBlockEntityNbt = json);
            }
        } catch (Throwable ignored) {
        }
    }

    public Integer getBlockId(JsonObject jsonObject) {
        if (jsonObject.has("b")) return jsonObject.get("b").getAsInt();
        if (!jsonObject.has("d")) return -1;
        String d = jsonObject.get("d").getAsString();
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(d);
        return blockTemplate.getId();
    }

    public Integer getOldBlockId(JsonObject jsonObject) {
        if (jsonObject.has("ob")) return jsonObject.get("ob").getAsInt();
        if (!jsonObject.has("od")) return -1;
        String d = jsonObject.get("od").getAsString();
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(d);
        return blockTemplate.getId();
    }

    @Override
    public String getDataString() {
        if (nexoBlockId != null) {
            return nexoBlockId;
        }
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(blockId);
        return blockTemplate.getDataBlock().getBlockDataString();
    }

    public String getOldDataString() {
        if (oldBlockData != null) return oldBlockData;
        if (oldBlockId == null || oldBlockId == 0) return null;
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(oldBlockId);
        return blockTemplate.getDataBlock().getBlockDataString();
    }

    @Override
    public String toSaveJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("b", blockId);
        if (oldBlockId != 0) {
            jsonObject.addProperty("ob", oldBlockId);
        }
        if (oldBlockData != null) {
            jsonObject.addProperty("od", oldBlockData);
        }
        if (nexoBlockId != null) {
            jsonObject.addProperty("nbId", nexoBlockId);
        }
        if (extraType != 0) {
            jsonObject.addProperty("xt", extraType);
        }
        if (extraData != null) {
            jsonObject.addProperty("xd", extraData);
        }
        if (blockEntityNbt != null) {
            jsonObject.addProperty("nbt", blockEntityNbt);
        }
        if (oldBlockEntityNbt != null) {
            jsonObject.addProperty("onbt", oldBlockEntityNbt);
        }
        return jsonObject.toString();
    }

}