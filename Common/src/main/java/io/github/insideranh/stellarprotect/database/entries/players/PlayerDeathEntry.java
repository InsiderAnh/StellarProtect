package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.DeathCause;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerDeathEntry extends LogEntry {

    private final byte cause;

    public PlayerDeathEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.cause = jsonObject.get("c").getAsByte();
    }

    public PlayerDeathEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.cause = jsonObject.get("c").getAsByte();
    }

    public PlayerDeathEntry(long playerId, Location location, byte cause) {
        super(playerId, ActionType.DEATH.getId(), location, System.currentTimeMillis());
        this.cause = cause;
    }

    @Override
    public String getDataString() {
        return DeathCause.getById(getCause()).name();
    }

    @Override
    public String toSaveJson() {
        return "{\"c\":" + cause + "}";
    }

}