package io.github.insideranh.stellarprotect.nms.v1_21;

import com.sk89q.worldedit.WorldEdit;
import io.github.insideranh.stellarprotect.api.WorldEditHandler;
import io.github.insideranh.stellarprotect.nms.v1_21.worldedit.StellarEditSessionEvent;
import lombok.Getter;
import org.bukkit.Bukkit;

public class WorldEditHandler_v1_21 implements WorldEditHandler {

    @Getter
    private static boolean fawe;

    public void load() {
        WorldEdit.getInstance().getEventBus().register(new StellarEditSessionEvent());
        fawe = Bukkit.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
        if (fawe) {
            Bukkit.getLogger().info("[StellarProtect] FastAsyncWorldEdit detected, enabling compatibility mode.");
        }
    }

}