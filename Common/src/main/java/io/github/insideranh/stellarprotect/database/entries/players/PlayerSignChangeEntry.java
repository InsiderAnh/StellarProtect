package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.Arrays;

@Getter
public class PlayerSignChangeEntry extends LogEntry {

    private final String[] lines;

    public PlayerSignChangeEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.lines = jsonObject.get("l").getAsString().split("\n");
    }

    @SneakyThrows
    public PlayerSignChangeEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.lines = jsonObject.get("l").getAsString().split("\n");
    }

    public PlayerSignChangeEntry(long playerId, Location location, String[] lines, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.lines = lines;
    }

    public String getLine(int index) {
        if (lines.length > index) {
            return lines[index];
        }
        return "";
    }

    @Override
    public String getDataString() {
        return Arrays.toString(lines);
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();

        PlayerSignChangeEntry entry = this;
        obj.addProperty("l", String.join("\n", entry.getLines()));

        return obj.toString();
    }

}