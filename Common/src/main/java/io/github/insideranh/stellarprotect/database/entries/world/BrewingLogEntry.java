package io.github.insideranh.stellarprotect.database.entries.world;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;

@Getter
public class BrewingLogEntry extends LogEntry {

    private final Long ingredientId;
    private final Long fuelId;
    private final Long resultId;
    private final Integer amount;

    public BrewingLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.ingredientId = jsonObject.has("i") ? jsonObject.get("i").getAsLong() : -1L;
        this.fuelId = jsonObject.has("f") ? jsonObject.get("f").getAsLong() : -1L;
        this.resultId = jsonObject.has("r") ? jsonObject.get("r").getAsLong() : -1L;
        this.amount = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 1;
        setAmount(this.amount);
        if (this.resultId > 0) setItemId(this.resultId);
    }

    public BrewingLogEntry(Player player, ItemStack ingredient, ItemStack fuel, ItemStack result) {
        super(player != null ? PlayerUtils.getPlayerOrConsoleId(player) : -2L,
                ActionType.BREWING.getId(),
                player != null ? player.getLocation() : new Location(null, 0, 0, 0),
                System.currentTimeMillis());
        long ingId = -1L;
        long fId = -1L;
        long rId = -1L;
        if (ingredient != null && !ingredient.getType().name().equals("AIR")) {
            ItemReference ing = StellarProtect.getInstance().getItemsManager().getItemReference(ingredient);
            ingId = ing.getTemplateId();
        }
        if (fuel != null && !fuel.getType().name().equals("AIR")) {
            ItemReference fl = StellarProtect.getInstance().getItemsManager().getItemReference(fuel);
            fId = fl.getTemplateId();
        }
        if (result != null && !result.getType().name().equals("AIR")) {
            ItemReference rs = StellarProtect.getInstance().getItemsManager().getItemReference(result);
            rId = rs.getTemplateId();
        }
        this.ingredientId = ingId;
        this.fuelId = fId;
        this.resultId = rId;
        this.amount = result != null ? result.getAmount() : 1;
        setItemId(this.resultId);
        setAmount(this.amount);
    }

    @Override
    public String toSaveJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("i", ingredientId);
        obj.addProperty("f", fuelId);
        obj.addProperty("r", resultId);
        obj.addProperty("a", amount);
        return obj.toString();
    }

}