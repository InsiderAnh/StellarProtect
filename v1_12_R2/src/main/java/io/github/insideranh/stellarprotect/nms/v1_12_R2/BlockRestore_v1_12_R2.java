package io.github.insideranh.stellarprotect.nms.v1_12_R2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.enums.ExtraDataType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BlockRestore_v1_12_R2 extends BlockRestore {

    public BlockRestore_v1_12_R2(String data) {
        super(data);
    }

    public BlockRestore_v1_12_R2(String data, byte extraType, String extraData) {
        super(data, extraType, extraData);
    }

    @Override
    public void reset(Gson gson, Location location) {
        Block block = location.getBlock();

        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        Material material = Material.getMaterial(jsonObject.get("m").getAsString());
        byte blockData = jsonObject.get("d").getAsByte();

        block.setType(material);
        try {
            block.setData(blockData);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if (extraData == null || extraType != ExtraDataType.INVENTORY_CONTENT.getId()) return;

        if (block.getState() instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) block.getState()).getInventory();
            JsonObject jsonInventory = new JsonParser().parse(extraData).getAsJsonObject();
            SerializerUtils.setInventoryContent(inventory, jsonInventory);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void preview(Player player, Gson gson, Location location) {
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
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