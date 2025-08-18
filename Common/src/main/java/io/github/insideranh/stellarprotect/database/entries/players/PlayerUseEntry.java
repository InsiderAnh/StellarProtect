package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerUseEntry extends LogEntry {

    private final String material;

    public PlayerUseEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.material = jsonObject.get("m").getAsString();
    }

    @SneakyThrows
    public PlayerUseEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.material = jsonObject.get("m").getAsString();
    }

    public PlayerUseEntry(long playerId, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        this.material = block.getType().name();
    }

    @Override
    public String getDataString() {
        return material;
    }

    @Override
    public String toSaveJson() {
        return "{\"m\":\"" + material + "\"}";
    }

}