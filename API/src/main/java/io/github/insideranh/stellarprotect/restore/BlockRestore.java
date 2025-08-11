package io.github.insideranh.stellarprotect.restore;

import com.google.gson.Gson;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@Getter
public abstract class BlockRestore {

    private final String data;

    protected BlockRestore(String data) {
        this.data = data;
    }

    public abstract void reset(Gson gson, Location location);

    public void remove(Location location) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
    }

}