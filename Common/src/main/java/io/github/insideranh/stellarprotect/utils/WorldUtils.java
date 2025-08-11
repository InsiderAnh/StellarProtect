package io.github.insideranh.stellarprotect.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import org.bukkit.Material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

public class WorldUtils {

    private static final BiMap<String, Integer> worldIds = HashBiMap.create();

    private static final HashSet<String> toggleableBlocks = new HashSet<>();
    private static final HashSet<String> legacyBlocks = new HashSet<>();
    private static final HashSet<String> interactableBlocks = new HashSet<>();
    private static final HashSet<String> endWithBlocks = new HashSet<>();

    private WorldUtils() {
        toggleableBlocks.add("DOOR");
        toggleableBlocks.add("TRAPDOOR");
        toggleableBlocks.add("FENCE_GATE");
        toggleableBlocks.add("BUTTON");

        endWithBlocks.add("CHEST");
        endWithBlocks.add("SHULKER_BOX");
        endWithBlocks.add("BED");
        endWithBlocks.add("DOOR");
        endWithBlocks.add("COMMAND_BLOCK");
        endWithBlocks.add("FURNACE");
        endWithBlocks.add("CAMPFIRE");

        interactableBlocks.add("STONECUTTER");
        interactableBlocks.add("DISPENSER");
        interactableBlocks.add("DROPPER");
        interactableBlocks.add("HOPPER");
        interactableBlocks.add("BREWING_STAND");
        interactableBlocks.add("ENCHANTING_TABLE");
        interactableBlocks.add("ANVIL");
        interactableBlocks.add("BEACON");
        interactableBlocks.add("BARREL");
        interactableBlocks.add("SMITHING_TABLE");
        interactableBlocks.add("CRAFTING_TABLE");
        interactableBlocks.add("CAULDRON");
        interactableBlocks.add("LOOM");
        interactableBlocks.add("SMOKER");
        interactableBlocks.add("FLETCHING_TABLE");
        interactableBlocks.add("GRINDSTONE");
        interactableBlocks.add("CARTOGRAPHY_TABLE");

        legacyBlocks.add("WORKBENCH");
        legacyBlocks.add("ENCHANTMENT_TABLE");
    }

    public static boolean isToggleableState(String block) {
        for (String endWithBlock : toggleableBlocks) {
            if (block.endsWith(endWithBlock)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInteractable(String block) {
        for (String interactableBlock : interactableBlocks) {
            if (block.equalsIgnoreCase(interactableBlock) || block.contains(interactableBlock)) {
                return true;
            }
        }
        for (String interactableBlock : legacyBlocks) {
            if (block.equalsIgnoreCase(interactableBlock) || block.contains(interactableBlock)) {
                return true;
            }
        }
        for (String endWithBlock : toggleableBlocks) {
            if (block.endsWith(endWithBlock)) {
                return true;
            }
        }
        for (String endWithBlock : endWithBlocks) {
            if (block.endsWith(endWithBlock)) {
                return true;
            }
        }
        return false;
    }

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
        return material == Material.CHEST ||
            material == Material.TRAPPED_CHEST ||
            material == Material.ENDER_CHEST ||
            material.name().contains("SHULKER_BOX") ||
            material.name().contains("BARREL");
    }

}