package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerBlockLogEntry extends LogEntry {

    private final int blockId;

    public PlayerBlockLogEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.blockId = getBlockId(jsonObject);
    }

    @SneakyThrows
    public PlayerBlockLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.blockId = getBlockId(jsonObject);
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        //this.data = StellarProtect.getInstance().getProtectNMS().getBlockData(block);
        BlockTemplate itemTemplate = StellarProtect.getInstance().getBlocksManager().getBlockTemplate(block.getBlockData());
        this.blockId = itemTemplate.getId();
    }

    public int getBlockId(JsonObject jsonObject) {
        if (jsonObject.has("b")) return jsonObject.get("b").getAsInt();
        if (!jsonObject.has("d")) return -1;

        String data = jsonObject.get("d").getAsString();
        BlockTemplate blockTemplate = StellarProtect.getInstance().getBlocksManager().getBlockTemplate(data);
        return blockTemplate.getId();
    }

    @Override
    public String getDataString() {
        BlockTemplate itemTemplate = StellarProtect.getInstance().getBlocksManager().getBlockTemplate(blockId);
        return itemTemplate.getBlockDataString();
    }

    @Override
    public String toSaveJson() {
        return "{\"b\":\"" + blockId + "\"}";
    }

}