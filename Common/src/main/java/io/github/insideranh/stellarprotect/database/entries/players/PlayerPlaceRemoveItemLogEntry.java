package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerPlaceRemoveItemLogEntry extends LogEntry {

    private final int blockId;
    private final long itemReferenceId;
    private final int amount;
    private final byte placed;

    public PlayerPlaceRemoveItemLogEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.blockId = getBlockId(jsonObject);

        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        this.placed = jsonObject.has("p") ? jsonObject.get("p").getAsByte() : 0;
    }

    @SneakyThrows
    public PlayerPlaceRemoveItemLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.blockId = getBlockId(jsonObject);

        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        this.placed = jsonObject.has("p") ? jsonObject.get("p").getAsByte() : 0;
    }

    public PlayerPlaceRemoveItemLogEntry(long playerId, ItemReference itemReference, Block block, boolean placed, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        BlockTemplate itemTemplate = StellarProtect.getInstance().getBlocksManager().getBlockTemplate(block);
        this.blockId = itemTemplate.getId();
        this.itemReferenceId = itemReference.getTemplateId();
        this.amount = itemReference.getAmount();
        this.placed = (byte) (placed ? 0 : 1);
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
        return itemTemplate.getDataBlock().getBlockDataString();
    }

    public boolean isPlaced() {
        return placed == 0;
    }

    @Override
    public String toSaveJson() {
        if (placed == 0) {
            return "{\"b\":\"" + blockId + "\"," +
                "\"id\":" + itemReferenceId + "," +
                "\"a\":" + amount + "}";
        }
        return "{\"b\":\"" + blockId + "\"," +
            "\"id\":" + itemReferenceId + "," +
            "\"a\":" + amount + "," +
            "\"p\":" + placed + "}";
    }

}