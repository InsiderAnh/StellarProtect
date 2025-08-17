package io.github.insideranh.stellarprotect.database.entries.economy;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.MoneyVarType;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Location;

import java.sql.ResultSet;

@Getter
public class PlayerEconomyEntry extends LogEntry {

    private final MoneyVarType variationType;
    private final double difference;

    public PlayerEconomyEntry(Document document, JsonObject jsonObject) {
        super(document);

        this.variationType = MoneyVarType.getById(jsonObject.get("v").getAsInt());
        this.difference = StringCleanerUtils.limitTo2Decimals(jsonObject.get("d").getAsDouble());
    }

    public PlayerEconomyEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);

        this.variationType = MoneyVarType.getById(jsonObject.get("v").getAsInt());
        this.difference = StringCleanerUtils.limitTo2Decimals(jsonObject.get("d").getAsDouble());
    }

    public PlayerEconomyEntry(long playerId, Location location, MoneyVarType variationType, double difference) {
        super(playerId, ActionType.MONEY.getId(), location, System.currentTimeMillis());
        this.variationType = variationType;
        this.difference = StringCleanerUtils.limitTo2Decimals(difference);
    }

    @Override
    public String toSaveJson() {
        return "{\"v\":" + variationType.getId() + ",\"d\":" + difference + "}";
    }

}