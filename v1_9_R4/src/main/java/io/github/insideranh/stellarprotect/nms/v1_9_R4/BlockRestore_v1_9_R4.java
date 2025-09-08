package io.github.insideranh.stellarprotect.nms.v1_9_R4;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockRestore_v1_9_R4 extends BlockRestore {

    public BlockRestore_v1_9_R4(String data) {
        super(data);
    }

    @Override
    public void reset(Gson gson, Location location) {
        Block block = location.getBlock();

        JsonObject jsonObject = gson.fromJson(getData(), JsonObject.class);
        Material material = Material.getMaterial(jsonObject.get("m").getAsString());
        byte blockData = jsonObject.get("d").getAsByte();

        block.setType(material);
        try {
            block.setData(blockData);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void preview(Player player, Gson gson, Location location) {
        JsonObject jsonObject = gson.fromJson(getData(), JsonObject.class);
        Material material = Material.getMaterial(jsonObject.get("m").getAsString());
        byte blockData = jsonObject.get("d").getAsByte();

        player.sendBlockChange(location, material, blockData);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void previewRemove(Player player, Location location) {
        player.sendBlockChange(location, Material.AIR, (byte) 0);
    }

}