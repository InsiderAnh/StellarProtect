package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerPlaceRemoveItemLogEntry extends LogEntry {

    private final String data;
    private final long itemReferenceId;
    private final int amount;
    private final byte placed;

    public PlayerPlaceRemoveItemLogEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.data = jsonObject.get("d").getAsString();
        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        this.placed = jsonObject.has("p") ? jsonObject.get("p").getAsByte() : 0;
    }

    @SneakyThrows
    public PlayerPlaceRemoveItemLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.data = jsonObject.get("d").getAsString();
        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        this.placed = jsonObject.has("p") ? jsonObject.get("p").getAsByte() : 0;
    }

    public PlayerPlaceRemoveItemLogEntry(long playerId, ItemReference itemReference, Block block, boolean placed, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        this.data = StellarProtect.getInstance().getProtectNMS().getBlockData(block);
        this.itemReferenceId = itemReference.getTemplateId();
        this.amount = itemReference.getAmount();
        this.placed = (byte) (placed ? 0 : 1);
    }

    @Override
    public String getDataString() {
        return data;
    }

    public boolean isPlaced() {
        return placed == 0;
    }

    @Override
    public String toSaveJson() {
        if (placed == 0) {
            return "{\"d\":\"" + SerializerUtils.escapeJson(getData()) + "\"," +
                "\"id\":" + itemReferenceId + "," +
                "\"a\":" + amount + "}";
        }
        return "{\"d\":\"" + SerializerUtils.escapeJson(getData()) + "\"," +
            "\"id\":" + itemReferenceId + "," +
            "\"a\":" + amount + "," +
            "\"p\":" + placed + "}";
    }

}