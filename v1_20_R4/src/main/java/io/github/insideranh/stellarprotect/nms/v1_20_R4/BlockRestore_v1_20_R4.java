package io.github.insideranh.stellarprotect.nms.v1_20_R4;

import com.google.gson.Gson;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class BlockRestore_v1_20_R4 extends BlockRestore {

    public BlockRestore_v1_20_R4(String data) {
        super(data);
    }

    @Override
    public void reset(Gson gson, Location location) {
        BlockData blockData = Bukkit.createBlockData(getData());

        Block block = location.getBlock();
        block.setBlockData(blockData);
    }

}