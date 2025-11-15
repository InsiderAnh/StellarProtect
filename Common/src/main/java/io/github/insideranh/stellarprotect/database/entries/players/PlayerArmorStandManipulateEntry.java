package io.github.insideranh.stellarprotect.database.entries.players;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.Getter;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerArmorStandManipulateEntry extends LogEntry {

    private final long oldItemId;
    private final long newItemId;
    private final int slot;

    public PlayerArmorStandManipulateEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.oldItemId = jsonObject.get("oi").getAsLong();
        this.newItemId = jsonObject.get("ni").getAsLong();
        this.slot = jsonObject.get("s").getAsInt();
    }

    public PlayerArmorStandManipulateEntry(long playerId, Location location, ItemReference oldItemReference, ItemReference newItemReference, int slot) {
        super(playerId, ActionType.ARMOR_STAND_MANIPULATE.getId(), WorldUtils.getShortId(location.getWorld().getName()), location.getX(), location.getY(), location.getZ(), System.currentTimeMillis());

        this.oldItemId = oldItemReference.getTemplateId();
        this.newItemId = newItemReference.getTemplateId();
        this.slot = slot;
    }

    @Override
    public String getDataString() {
        ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(oldItemId);
        return itemTemplate.getBukkitItem().getType().name();
    }

    public String getNewDataString() {
        ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(newItemId);
        return itemTemplate.getBukkitItem().getType().name();
    }

    @Override
    public String toSaveJson() {
        return "{\"oi\":\"" + oldItemId + "\",\"ni\":\"" + newItemId + "\",\"s\":\"" + slot + "\"}";
    }

}
