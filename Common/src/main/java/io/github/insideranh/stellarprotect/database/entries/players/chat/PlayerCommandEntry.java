package io.github.insideranh.stellarprotect.database.entries.players.chat;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

@Getter
public class PlayerCommandEntry extends LogEntry {

    private final String command;
    // 0 = server, 1 = player
    private final byte type;

    public PlayerCommandEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.command = jsonObject.get("c").getAsString();
        this.type = jsonObject.get("t").getAsByte();
    }

    public PlayerCommandEntry(long playerId, String command) {
        super(playerId, ActionType.COMMAND.getId(), 0, 0, 0, 0, System.currentTimeMillis());
        this.command = command;
        this.type = 0;
    }

    public PlayerCommandEntry(long playerId, Player player, String command) {
        super(playerId, ActionType.COMMAND.getId(), player.getLocation(), System.currentTimeMillis());
        this.command = command;
        this.type = 1;
    }

    @Override
    public String getDataString() {
        return command;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerCommandEntry entry = this;
        obj.addProperty("c", entry.getCommand());
        obj.addProperty("t", entry.getType());

        return obj.toString();
    }

}