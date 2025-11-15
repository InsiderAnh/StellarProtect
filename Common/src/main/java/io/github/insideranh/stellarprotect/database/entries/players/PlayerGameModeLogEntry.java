package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerGameModeLogEntry extends LogEntry {

    private final byte lastGameMode;
    private final byte newGameMode;

    @SneakyThrows
    public PlayerGameModeLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.lastGameMode = jsonObject.has("l") ? jsonObject.get("l").getAsByte() : 0;
        this.newGameMode = jsonObject.has("n") ? jsonObject.get("n").getAsByte() : 0;
    }

    public PlayerGameModeLogEntry(long playerId, Location location, int lastGameMode, int newGameMode) {
        super(playerId, ActionType.GAME_MODE.getId(), location, System.currentTimeMillis());
        this.lastGameMode = (byte) lastGameMode;
        this.newGameMode = (byte) newGameMode;
    }

    @Override
    public String getDataString() {
        return lastGameMode + ";" + newGameMode;
    }

    @Override
    public String toSaveJson() {
        return "{\"l\":" + lastGameMode + ",\"n\":" + newGameMode + "}";
    }

}