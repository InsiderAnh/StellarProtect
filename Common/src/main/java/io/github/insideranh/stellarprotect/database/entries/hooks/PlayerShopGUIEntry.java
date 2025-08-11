package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerShopGUIEntry extends LogEntry {

    private final long itemId;
    private final int amount;
    private final double price;
    private final byte shopAction;

    public PlayerShopGUIEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.itemId = jsonObject.get("i").getAsLong();
        this.amount = jsonObject.get("a").getAsInt();
        this.price = jsonObject.get("p").getAsDouble();
        this.shopAction = jsonObject.get("s").getAsByte();
    }

    public PlayerShopGUIEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.itemId = jsonObject.get("i").getAsLong();
        this.amount = jsonObject.get("a").getAsInt();
        this.price = jsonObject.get("p").getAsDouble();
        this.shopAction = jsonObject.get("s").getAsByte();
    }

    public PlayerShopGUIEntry(long playerId, Location location, ItemReference itemReference, int amount, double price, byte shopAction) {
        super(playerId, ActionType.SHOP_GUI.getId(), location, System.currentTimeMillis());
        this.itemId = itemReference.getTemplateId();
        this.amount = amount;
        this.price = price;
        this.shopAction = shopAction;
    }

    @Override
    public String getDataString() {
        ItemTemplate itemTemplate = StellarProtect.getInstance().getItemsManager().getItemTemplate(itemId);
        return itemTemplate.getBukkitItem().getType().name();
    }

}
