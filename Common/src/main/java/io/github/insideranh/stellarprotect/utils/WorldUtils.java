package io.github.insideranh.stellarprotect.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import org.bukkit.Material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class WorldUtils {

    private static final BiMap<String, Integer> worldIds = HashBiMap.create();

    public static String getFormatedLocation(LocationCache cache) {
        return "x" + cache.getX() + "/y" + cache.getY() + "/z" + cache.getZ() + "/" + getWorld(cache.getWorldId());
    }

    public static String getWorld(int id) {
        return worldIds.inverse().get(id);
    }

    public static void cacheWorld(String world, int id) {
        worldIds.put(world, id);
    }

    public static int getShortId(String world) {
        if (worldIds.containsKey(world)) {
            return worldIds.get(world);
        }
        int shortId = generateShortId(world);
        worldIds.put(world, shortId);

        StellarProtect.getInstance().getProtectDatabase().saveWorld(world, shortId);

        return shortId;
    }

    private static int generateShortId(String word) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(word.getBytes(StandardCharsets.UTF_8));

            int value = 0;
            for (int i = 0; i < 4; i++) {
                value = (value << 8) | (hash[i] & 0xFF);
            }

            return Math.abs(value) % 10000;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible");
        }
    }

    public static boolean isValidChestBlock(Material material) {
        return material.equals(Material.CHEST) ||
            material.equals(Material.TRAPPED_CHEST) ||
            material.equals(Material.ENDER_CHEST) ||
            material.equals(Material.DROPPER) ||
            material.name().contains("SHULKER_BOX") ||
            material.name().contains("BARREL");
    }

}