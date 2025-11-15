package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.sql.ResultSet;

@Getter
public class PlayerMountEntry extends LogEntry {

    // mount = 0 (0 = mount, 1 = dismount)
    private final String entityType;
    private final byte mount;

    public PlayerMountEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.entityType = jsonObject.get("et").getAsString();
        this.mount = jsonObject.get("mt").getAsByte();
    }

    public PlayerMountEntry(long playerId, Location location, Entity entity, boolean mount) {
        super(playerId, ActionType.MOUNT.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.entityType = entity.getType().name();
        this.mount = (byte) (mount ? 1 : 0);
    }

    @Override
    public String getDataString() {
        return entityType;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerMountEntry entry = this;
        obj.addProperty("et", entry.getEntityType());
        obj.addProperty("mt", entry.getMount());

        return obj.toString();
    }

}
