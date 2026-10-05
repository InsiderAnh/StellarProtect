package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;

@Getter
public class PlayerInventorySnapshotEntry extends LogEntry {

    private final ItemStack[] contents;
    private final String rawBase64;

    @SneakyThrows
    public PlayerInventorySnapshotEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        String raw = jsonObject.has("d") ? jsonObject.get("d").getAsString() : "";
        this.rawBase64 = raw;
        this.contents = raw.isEmpty() ? new ItemStack[0] : io.github.insideranh.stellarprotect.utils.InventorySerializable.itemStackArrayFromBase64(raw);
    }

    public PlayerInventorySnapshotEntry(long playerId, ItemStack[] contents, Location location, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.contents = contents == null ? new ItemStack[0] : contents;
        this.rawBase64 = io.github.insideranh.stellarprotect.utils.InventorySerializable.itemStackArrayToBase64(this.contents);
        setAmount(this.contents.length);
    }

    @Override
    public String getDataString() {
        return "snapshot: " + contents.length + " slots";
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("d", rawBase64);
        obj.addProperty("n", contents.length);
        return obj.toString();
    }

}