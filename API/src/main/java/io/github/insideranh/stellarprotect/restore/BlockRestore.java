package io.github.insideranh.stellarprotect.restore;

import com.google.gson.Gson;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Getter
public abstract class BlockRestore {

    protected final String data;
    protected final byte extraType;
    protected final String extraData;

    protected BlockRestore(String data) {
        this.data = data;
        this.extraType = 0;
        this.extraData = null;
    }

    protected BlockRestore(String data, byte extraType, String extraData) {
        this.data = data;
        this.extraType = extraType;
        this.extraData = extraData;
    }

    public abstract void reset(Gson gson, Location location);

    public abstract void preview(Player player, Gson gson, Location location);

    public abstract void previewRemove(Player player, Location location);

    public void remove(Location location) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
    }

}