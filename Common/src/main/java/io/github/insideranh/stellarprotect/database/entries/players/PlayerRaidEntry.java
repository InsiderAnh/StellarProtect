package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

public class PlayerRaidEntry extends LogEntry {

    public PlayerRaidEntry(Document document, JsonObject jsonObject) {
        super(document);
    }

    public PlayerRaidEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

    }

    public PlayerRaidEntry(long playerId, Location location, double difference) {
        super(playerId, ActionType.RAID.getId(), location, System.currentTimeMillis());
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerRaidEntry entry = this;

        return obj.toString();
    }

}