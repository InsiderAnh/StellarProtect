package io.github.insideranh.stellarprotect.database.entries.world;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import lombok.Getter;
import org.bukkit.block.Block;

import java.sql.ResultSet;

@Getter
public class CropGrowLogEntry extends LogEntry {

    private final int age;

    public CropGrowLogEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
        this.age = jsonObject.has("a") ? jsonObject.get("a").getAsInt() : 0;
    }

    public CropGrowLogEntry(long playerId, Block block) {
        super(playerId, ActionType.CROP_GROW.getId(), block.getLocation(), System.currentTimeMillis());
        this.age = StellarProtect.getInstance().getProtectNMS().getAge(block);
    }

    public CropGrowLogEntry(Block block) {
        super(PlayerUtils.getEntityByDirectId("=natural"), ActionType.CROP_GROW.getId(), block.getLocation(), System.currentTimeMillis());
        this.age = StellarProtect.getInstance().getProtectNMS().getAge(block);
    }

    @Override
    public String toSaveJson() {
        if (age == 0) {
            return "";
        }

        return "{\"a\":" + age + "}";
    }

}