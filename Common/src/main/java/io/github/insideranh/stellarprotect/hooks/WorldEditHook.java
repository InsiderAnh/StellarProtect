package io.github.insideranh.stellarprotect.hooks;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.WorldEditHandler;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WorldEditHook {

    private final WorldEditLogListenerImpl logListener;

    public WorldEditHook() {
        logListener = new WorldEditLogListenerImpl();
        WorldEditHandler worldEditHandler = StellarProtect.getInstance().getWorldEditHandler();
        if (worldEditHandler != null) {
            worldEditHandler.load();
        }
    }

    public RadiusArg getRadiusArgWorldEdit(Player player) {
        try {
            WorldEditPlugin worldEdit = getWorldEdit();
            if (worldEdit == null) {
                return null;
            }

            LocalSession session = worldEdit.getSession(player);
            World world = session.getSelectionWorld();

            if (world == null) {
                return null;
            }

            Region region = session.getSelection(world);
            if (region == null || !world.getName().equals(player.getWorld().getName())) {
                return null;
            }

            BlockVector3 minPoint = region.getMinimumPoint();
            int xMin = minPoint.x();
            int yMin = minPoint.y();
            int zMin = minPoint.z();

            int width = region.getWidth();
            int height = region.getHeight();
            int length = region.getLength();

            int radius = Math.max(Math.max(width, height), length);

            int xMax = xMin + (width - 1);
            int yMax = yMin + (height - 1);
            int zMax = zMin + (length - 1);

            return new RadiusArg(WorldUtils.searchWorldId(world.getName()), radius,
                xMin, xMax, yMin, yMax, zMin, zMax);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public WorldEditPlugin getWorldEdit() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (!(plugin instanceof WorldEditPlugin)) {
            return null;
        }

        return (WorldEditPlugin) plugin;
    }

    public void cleanup() {
        if (logListener != null) {
            logListener.unregister();
            Bukkit.getLogger().info("[StellarProtect] WorldEdit logging system unregistered");
        }
    }

}