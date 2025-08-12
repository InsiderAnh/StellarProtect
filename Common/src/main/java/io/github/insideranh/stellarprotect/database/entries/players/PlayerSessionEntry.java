package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerSessionEntry extends LogEntry {

    private final byte login;
    private final long sessionTime;

    public PlayerSessionEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.login = jsonObject.get("s").getAsByte();
        this.sessionTime = jsonObject.has("t") ? jsonObject.get("t").getAsLong() : 0;
    }

    @SneakyThrows
    public PlayerSessionEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.login = jsonObject.get("s").getAsByte();
        this.sessionTime = jsonObject.has("t") ? jsonObject.get("t").getAsLong() : 0;
    }

    public PlayerSessionEntry(long playerId, Location location, byte login, long sessionTime) {
        super(playerId, ActionType.SESSION.getId(), location, System.currentTimeMillis());
        this.login = login;
        this.sessionTime = sessionTime;
    }

    @Override
    public String getDataString() {
        return String.valueOf(login);
    }

    @Override
    public String toSaveJson() {
        if (sessionTime != 0) {
            return "{\"s\":" + login + ",\"t\":" + sessionTime + "}";
        } else {
            return "{\"s\":" + login + "}";
        }
    }

}