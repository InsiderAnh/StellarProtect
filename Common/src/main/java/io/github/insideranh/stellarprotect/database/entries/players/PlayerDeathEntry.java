package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.DeathCause;
import lombok.Getter;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Getter
public class PlayerDeathEntry extends LogEntry {

    private final byte cause;
    // ID item -> amount
    private Map<Long, Integer> drops = new HashMap<>();

    public PlayerDeathEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.cause = jsonObject.get("c").getAsByte();

        if (jsonObject.has("d")) {
            JsonObject addedItemsObj = jsonObject.get("d").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : addedItemsObj.entrySet()) {
                String base64Key = entry.getKey();
                int amount = entry.getValue().getAsInt();
                this.drops.put(Long.parseLong(base64Key), amount);
            }
        }
    }

    public PlayerDeathEntry(long playerId, Location location, byte cause, Map<Long, Integer> drops) {
        super(playerId, ActionType.DEATH.getId(), location, System.currentTimeMillis());
        this.cause = cause;
        this.drops = drops;
    }

    @Override
    public String getDataString() {
        return DeathCause.getById(getCause()).name();
    }

    @Override
    public String toSaveJson() {
        if (drops.isEmpty()) {
            return "{\"c\":" + cause + "}";
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("c", cause);

        JsonObject dropItemsOjb = new JsonObject();
        for (Map.Entry<Long, Integer> addedItem : getDrops().entrySet()) {
            dropItemsOjb.addProperty(String.valueOf(addedItem.getKey()), addedItem.getValue());
        }

        obj.add("d", dropItemsOjb);
        return obj.toString();
    }

}