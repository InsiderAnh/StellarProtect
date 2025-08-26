package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;
import java.util.Objects;

@Getter
public class PlayerItemLogEntry extends LogEntry {

    private final long itemReferenceId;
    private final int amount;

    public PlayerItemLogEntry(Document document, JsonObject jsonObject) {
        super(document);
        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
    }

    @SneakyThrows
    public PlayerItemLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.itemReferenceId = jsonObject.has("id") ? jsonObject.get("id").getAsLong() : jsonObject.get("it64").getAsLong();
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
    }

    public PlayerItemLogEntry(long playerId, ItemReference itemReference, Location location, ActionType actionType) {
        super(playerId, actionType.getId(), location, System.currentTimeMillis());
        this.itemReferenceId = itemReference.getTemplateId();
        this.amount = itemReference.getAmount();
    }

    @Override
    public String getDataString() {
        ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(itemReferenceId);
        return itemTemplate.getBukkitItem().getType().name();
    }

    @Override
    public String toSaveJson() {
        return "{\"id\":" + itemReferenceId + ",\"a\":" + amount + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlayerItemLogEntry that = (PlayerItemLogEntry) o;
        return itemReferenceId == that.itemReferenceId && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), itemReferenceId, amount);
    }

}