package io.github.insideranh.stellarprotect.nms.v1_12_R2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.blocks.DataBlock;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.Objects;

public class DataBlock_v1_12_R2 implements DataBlock {

    private static final Gson gson = new Gson();
    private final Material material;
    private final byte data;
    @Getter
    private final String blockDataString;

    public DataBlock_v1_12_R2(String blockDataString) {
        this.blockDataString = blockDataString;
        JsonObject jsonObject = gson.fromJson(blockDataString, JsonObject.class);
        this.material = Material.getMaterial(jsonObject.get("m").getAsString());
        this.data = jsonObject.get("d").getAsByte();
    }

    @SuppressWarnings("deprecation")
    public DataBlock_v1_12_R2(Block block) {
        this.material = block.getType();
        this.data = block.getData();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("m", material.name());
        jsonObject.addProperty("d", data);

        this.blockDataString = jsonObject.toString();
    }

    @SuppressWarnings("deprecation")
    public DataBlock_v1_12_R2(BlockState block) {
        this.material = block.getType();
        this.data = block.getData().getData();

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("m", material.name());
        jsonObject.addProperty("d", data);

        this.blockDataString = jsonObject.toString();
    }

    @Override
    public String getTypeMaterial() {
        return material.name();
    }

    @Override
    public int hashCode() {
        return this.blockDataString.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataBlock_v1_12_R2 that = (DataBlock_v1_12_R2) o;
        return Objects.equals(blockDataString, that.blockDataString);
    }

}