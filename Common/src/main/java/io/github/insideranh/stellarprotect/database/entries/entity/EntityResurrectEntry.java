package io.github.insideranh.stellarprotect.database.entries.entity;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class EntityResurrectEntry extends LogEntry {

    private final String hand;

    public EntityResurrectEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.hand = jsonObject.has("h") ? jsonObject.get("h").getAsString() : "";
    }

    @SneakyThrows
    public EntityResurrectEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.hand = jsonObject.has("h") ? jsonObject.get("h").getAsString() : "";
    }

    public EntityResurrectEntry(long playerId, Location location, String hand, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.hand = hand;
    }

    @Override
    public String getDataString() {
        return hand;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        obj.addProperty("h", hand);

        return obj.toString();
    }

}