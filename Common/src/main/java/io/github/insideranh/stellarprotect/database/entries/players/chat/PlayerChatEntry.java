package io.github.insideranh.stellarprotect.database.entries.players.chat;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

@Getter
public class PlayerChatEntry extends LogEntry {

    private final String message;

    public PlayerChatEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.message = jsonObject.get("m").getAsString();
    }

    public PlayerChatEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.message = jsonObject.get("m").getAsString();
    }

    public PlayerChatEntry(long playerId, Player player, String message) {
        super(playerId, ActionType.CHAT.getId(), player.getLocation(), System.currentTimeMillis());
        this.message = message;
    }

    @Override
    public String getDataString() {
        return message;
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerChatEntry entry = this;
        obj.addProperty("m", entry.getMessage());

        return obj.toString();
    }

}