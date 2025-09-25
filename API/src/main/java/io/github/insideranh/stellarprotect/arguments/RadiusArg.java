package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@Getter
public class RadiusArg {

    private final int worldId;
    private final double radius;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;
    private final double minZ;
    private final double maxZ;

    public RadiusArg(Location location, double radius, int worldId) {
        this(worldId, radius, location.getBlockX() - radius, location.getBlockX() + radius, location.getBlockY() - radius, location.getBlockY() + radius, location.getBlockZ() - radius, location.getBlockZ() + radius);
    }

    public RadiusArg(int worldId, double radius, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        this.worldId = worldId;
        this.radius = radius;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;

        Bukkit.getLogger().info(this.toString());
    }

    @Override
    public String toString() {
        return "RadiusArg{" +
            "worldId=" + worldId +
            ", radius=" + radius +
            ", minX=" + minX +
            ", maxX=" + maxX +
            ", minY=" + minY +
            ", maxY=" + maxY +
            ", minZ=" + minZ +
            ", maxZ=" + maxZ +
            '}';
    }

}