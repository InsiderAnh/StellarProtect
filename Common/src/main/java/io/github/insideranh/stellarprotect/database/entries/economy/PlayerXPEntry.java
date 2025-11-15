package io.github.insideranh.stellarprotect.database.entries.economy;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerXPEntry extends LogEntry {

    private final double difference;

    public PlayerXPEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.difference = jsonObject.get("d").getAsDouble();
    }

    public PlayerXPEntry(long playerId, Location location, double difference) {
        super(playerId, ActionType.XP.getId(), location, System.currentTimeMillis());
        this.difference = difference;
    }

    @Override
    public String toSaveJson() {
        return "{\"d\":" + difference + "}";
    }

}