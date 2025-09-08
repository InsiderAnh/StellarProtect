package io.github.insideranh.stellarprotect.nms.v1_17_R1;

import com.google.gson.Gson;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class BlockRestore_v1_17_R1 extends BlockRestore {

    public BlockRestore_v1_17_R1(String data) {
        super(data);
    }

    @Override
    public void reset(Gson gson, Location location) {
        BlockData blockData = Bukkit.createBlockData(getData());

        Block block = location.getBlock();
        block.setBlockData(blockData);
    }

    @Override
    public void preview(Player player, Gson gson, Location location) {
        BlockData blockData = Bukkit.createBlockData(getData());
        player.sendBlockChange(location, blockData);
    }

    @Override
    public void previewRemove(Player player, Location location) {
        player.sendBlockChange(location, Material.AIR.createBlockData());
    }

}