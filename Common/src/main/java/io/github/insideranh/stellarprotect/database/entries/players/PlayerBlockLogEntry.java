package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.managers.BlocksManager;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerBlockLogEntry extends LogEntry {

    private static final BlocksManager blocksManager = StellarProtect.getInstance().getBlocksManager();
    private final int blockId;
    private String nexoBlockId;

    public PlayerBlockLogEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.blockId = getBlockId(jsonObject);
        if (jsonObject.has("nbId")) {
            this.nexoBlockId = jsonObject.get("nbId").getAsString();
        }
    }

    @SneakyThrows
    public PlayerBlockLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.blockId = getBlockId(jsonObject);
        if (jsonObject.has("nbId")) {
            this.nexoBlockId = jsonObject.get("nbId").getAsString();
        }
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        BlockTemplate itemTemplate = blocksManager.getBlockTemplate(block);
        this.blockId = itemTemplate.getId();
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType, String nexoBlockId) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        BlockTemplate itemTemplate = blocksManager.getBlockTemplate(block);
        this.blockId = itemTemplate.getId();
        this.nexoBlockId = nexoBlockId;
    }

    public int getBlockId(JsonObject jsonObject) {
        if (jsonObject.has("b")) return jsonObject.get("b").getAsInt();
        if (!jsonObject.has("d")) return -1;

        String data = jsonObject.get("d").getAsString();
        BlockTemplate blockTemplate = blocksManager.getBlockTemplate(data);
        return blockTemplate.getId();
    }

    @Override
    public String getDataString() {
        if (nexoBlockId != null) {
            return nexoBlockId;
        }

        BlockTemplate itemTemplate = blocksManager.getBlockTemplate(blockId);
        return itemTemplate.getDataBlock().getBlockDataString();
    }

    @Override
    public String toSaveJson() {
        if (nexoBlockId != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("b", blockId);
            jsonObject.addProperty("nbId", nexoBlockId);
            return jsonObject.toString();
        }
        return "{\"b\":\"" + blockId + "\"}";
    }

}