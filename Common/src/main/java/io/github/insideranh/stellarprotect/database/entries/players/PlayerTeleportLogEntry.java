package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.TeleportType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerTeleportLogEntry extends LogEntry {

    private final float yaw;
    private final float pitch;
    private final int teleportType;

    @SneakyThrows
    public PlayerTeleportLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.yaw = jsonObject.has("y") ? jsonObject.get("y").getAsFloat() : 0;
        this.pitch = jsonObject.has("p") ? jsonObject.get("p").getAsFloat() : 0;
        this.teleportType = jsonObject.has("t") ? jsonObject.get("t").getAsInt() : TeleportType.UNKNOWN.getId();
    }

    public PlayerTeleportLogEntry(long playerId, Location location, TeleportType teleportType) {
        super(playerId, ActionType.TELEPORT.getId(), location, System.currentTimeMillis());
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.teleportType = teleportType.getId();
    }

    @Override
    public String getDataString() {
        return TeleportType.getById(teleportType).name();
    }

    @Override
    public String toSaveJson() {
        return "{\"y\":" + yaw + ",\"p\":" + pitch + ",\"t\":" + teleportType + "}";
    }

}