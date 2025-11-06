package io.github.insideranh.stellarprotect.nms.v1_21_R8;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.enums.ExtraDataType;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.utils.SerializerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BlockRestore_v1_21_R8 extends BlockRestore {

    public BlockRestore_v1_21_R8(String data) {
        super(data);
    }

    public BlockRestore_v1_21_R8(String data, byte extraType, String extraData) {
        super(data, extraType, extraData);
    }

    @Override
    public void reset(Gson gson, Location location) {
        BlockData blockData = Bukkit.createBlockData(data);

        Block block = location.getBlock();
        block.setBlockData(blockData, false);

        if (extraData == null || extraType != ExtraDataType.INVENTORY_CONTENT.getId()) return;

        if (block.getState() instanceof InventoryHolder) {
            Inventory inventory = ((InventoryHolder) block.getState()).getInventory();
            JsonObject jsonObject = new JsonParser().parse(extraData).getAsJsonObject();
            SerializerUtils.setInventoryContent(inventory, jsonObject);
        }
    }

    @Override
    public void preview(Player player, Gson gson, Location location) {
        BlockData blockData = Bukkit.createBlockData(data);
        player.sendBlockChange(location, blockData);
    }

    @Override
    public void previewRemove(Player player, Location location) {
        player.sendBlockChange(location, Material.AIR.createBlockData());
    }

}