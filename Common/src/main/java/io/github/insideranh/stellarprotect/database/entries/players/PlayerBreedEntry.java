package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.entity.Entity;

import java.sql.ResultSet;

@Getter
public class PlayerBreedEntry extends LogEntry {

    private final String entityUUID;
    private final String entityType;

    public PlayerBreedEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.entityUUID = jsonObject.get("eu").getAsString();
        this.entityType = jsonObject.get("et").getAsString();
    }

    public PlayerBreedEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.entityUUID = jsonObject.get("eu").getAsString();
        this.entityType = jsonObject.get("et").getAsString();
    }

    public PlayerBreedEntry(long playerId, Entity tamed) {
        super(playerId, ActionType.BREED.getId(), tamed.getLocation(), System.currentTimeMillis());
        this.entityType = tamed.getType().name();
        this.entityUUID = tamed.getUniqueId().toString();
    }

    @Override
    public String toSaveJson() {
        return "{\"eu\":\"" + entityUUID + "\",\"et\":\"" + entityType + "\"}";
    }

}