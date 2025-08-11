package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class PlayerBlockLogEntry extends LogEntry {

    private final String data;

    public PlayerBlockLogEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.data = jsonObject.get("d").getAsString();
    }

    @SneakyThrows
    public PlayerBlockLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.data = jsonObject.get("d").getAsString();
    }

    public PlayerBlockLogEntry(long playerId, Block block, ActionType actionType) {
        super(playerId, actionType.getId(), block.getLocation(), System.currentTimeMillis());
        this.data = StellarProtect.getInstance().getProtectNMS().getBlockData(block);
    }

    @Override
    public String getDataString() {
        return data;
    }

    @Override
    public String toSaveJson() {
        return "{\"d\":\"" + SerializerUtils.escapeJson(getData()) + "\"}";
    }

}