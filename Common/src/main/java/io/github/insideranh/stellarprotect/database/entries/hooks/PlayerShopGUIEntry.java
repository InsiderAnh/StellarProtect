package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

public class PlayerShopGUIEntry extends LogEntry {

    private final long itemId;
    private final int amount;
    private final byte shopAction;

    public PlayerShopGUIEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.itemId = jsonObject.get("i").getAsLong();
        this.amount = jsonObject.get("a").getAsInt();
        this.shopAction = jsonObject.get("s").getAsByte();
    }

    public PlayerShopGUIEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.itemId = jsonObject.get("i").getAsLong();
        this.amount = jsonObject.get("a").getAsInt();
        this.shopAction = jsonObject.get("s").getAsByte();
    }

    public PlayerShopGUIEntry(long playerId, Location location, ItemReference itemReference, byte shopAction) {
        super(playerId, ActionType.SHOP_GUI.getId(), location, System.currentTimeMillis());
        this.itemId = itemReference.getTemplateId();
        this.amount = itemReference.getAmount();
        this.shopAction = shopAction;
    }

}
