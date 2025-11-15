package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.managers.BlocksManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.sql.ResultSet;

@Getter
public class PlayerBlockStateLogEntry extends LogEntry {

    private static final BlocksManager blocksManager = StellarProtect.getInstance().getBlocksManager();
    private final int lastBlockId;
    private final int newBlockId;

    @SneakyThrows
    public PlayerBlockStateLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.newBlockId = jsonObject.has("nb") ? jsonObject.get("nb").getAsInt() : -1;
        this.lastBlockId = jsonObject.has("lb") ? jsonObject.get("lb").getAsInt() : -1;
    }

    public PlayerBlockStateLogEntry(long playerId, BlockState blockState, BlockState newState, ActionType actionType) {
        super(playerId, actionType.getId(), blockState.getLocation(), System.currentTimeMillis());
        BlockTemplate lastTemplate = blocksManager.getBlockTemplate(blockState);
        BlockTemplate newTemplate = blocksManager.getBlockTemplate(newState);
        this.lastBlockId = lastTemplate.getId();
        this.newBlockId = newTemplate.getId();
    }

    public PlayerBlockStateLogEntry(long playerId, Location location, Block block, Block newBlock, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        BlockTemplate lastTemplate = blocksManager.getBlockTemplate(block);
        BlockTemplate newTemplate = blocksManager.getBlockTemplate(newBlock);
        this.lastBlockId = lastTemplate.getId();
        this.newBlockId = newTemplate.getId();
    }

    public PlayerBlockStateLogEntry(long playerId, Location location, BlockState blockState, BlockState newState, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        BlockTemplate lastTemplate = blocksManager.getBlockTemplate(blockState);
        BlockTemplate newTemplate = blocksManager.getBlockTemplate(newState);
        this.lastBlockId = lastTemplate.getId();
        this.newBlockId = newTemplate.getId();
    }

    @Override
    public String getDataString() {
        BlockTemplate itemTemplate = blocksManager.getBlockTemplate(newBlockId);
        return itemTemplate.getDataBlock().getBlockDataString();
    }

    public String lastDataString() {
        BlockTemplate itemTemplate = blocksManager.getBlockTemplate(lastBlockId);
        return itemTemplate.getDataBlock().getBlockDataString();
    }

    @Override
    public String toSaveJson() {
        return "{\"nb\":\"" + newBlockId + "\",\"lb\":\"" + lastBlockId + "\"}";
    }

}