package io.github.insideranh.stellarprotect.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@UtilityClass
public class LocationUtils {

    public static String getStringLocation(@NonNull Location location) {
        if (location.getWorld() == null) return null;
        return location.getWorld().getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ();
    }

    public static String getFormattedStringLocation(@NonNull Location location) {
        if (location.getWorld() == null) return "";
        return "x" + location.getBlockX() + "/y" + location.getBlockY() + "/z" + location.getBlockZ() + "/" + location.getWorld().getName();
    }

    public static Location getLocationString(@NonNull String location) {
        String[] split = location.split(";");

        World world = Bukkit.getWorld(split[0]);
        if (world == null) return null;

        return new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }

}