package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PlayerTransactionEntry extends LogEntry {

    private Map<Long, Integer> added = new HashMap<>();
    private Map<Long, Integer> removed = new HashMap<>();

    public PlayerTransactionEntry(Document document, JsonObject jsonObject) {
        super(document);

        if (jsonObject.has("ai")) {
            JsonObject addedItemsObj = jsonObject.get("ai").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : addedItemsObj.entrySet()) {
                String base64Key = entry.getKey();
                int amount = entry.getValue().getAsInt();
                this.added.put(Long.parseLong(base64Key), amount);
            }
        }
        if (jsonObject.has("ri")) {
            JsonObject removedItemsObj = jsonObject.get("ri").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : removedItemsObj.entrySet()) {
                String base64Key = entry.getKey();
                int amount = entry.getValue().getAsInt();
                this.removed.put(Long.parseLong(base64Key), amount);
            }
        }
    }

    @SneakyThrows
    public PlayerTransactionEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        if (jsonObject.has("ai")) {
            JsonObject addedItemsObj = jsonObject.get("ai").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : addedItemsObj.entrySet()) {
                String base64Key = entry.getKey();
                int amount = entry.getValue().getAsInt();
                this.added.put(Long.parseLong(base64Key), amount);
            }
        }

        if (jsonObject.has("ri")) {
            JsonObject removedItemsObj = jsonObject.get("ri").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : removedItemsObj.entrySet()) {
                String base64Key = entry.getKey();
                int amount = entry.getValue().getAsInt();
                this.removed.put(Long.parseLong(base64Key), amount);
            }
        }
    }

    public PlayerTransactionEntry(long playerId, Map<Long, Integer> added, Map<Long, Integer> removed, Location location, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.added = added;
        this.removed = removed;
    }

    @Override
    public String getDataString() {
        return "added: " + added.toString() + ", removed: " + removed.toString();
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerTransactionEntry entry = this;

        JsonObject addedItemsObj = new JsonObject();
        for (Map.Entry<Long, Integer> addedItem : entry.getAdded().entrySet()) {
            addedItemsObj.addProperty(String.valueOf(addedItem.getKey()), addedItem.getValue());
        }

        JsonObject removedItemsObj = new JsonObject();
        for (Map.Entry<Long, Integer> removedItem : entry.getRemoved().entrySet()) {
            removedItemsObj.addProperty(String.valueOf(removedItem.getKey()), removedItem.getValue());
        }

        obj.add("ai", addedItemsObj);
        obj.add("ri", removedItemsObj);

        return obj.toString();
    }

}