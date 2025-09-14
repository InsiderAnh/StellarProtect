package io.github.insideranh.stellarprotect.trackers;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockTracker {

    private static final byte[] bitCache = new byte[Material.values().length];
    private static final byte TOGGLEABLE_FLAG = 1;    // 00000001
    private static final byte PLACEABLE_FLAG = 2;     // 00000010
    private static final byte VINE_FLAG = 4;          // 00000100
    private static final byte SCULK_FLAG = 8;         // 00001000
    private static final byte CHORUS_FLAG = 16;       // 00010000
    private static final byte BAMBOO_FLAG = 32;       // 00100000
    private static final byte AMETHYST_FLAG = 64;     // 01000000
    private static boolean initialized = false;

    public static void initializeCache() {
        if (initialized) return;

        Arrays.fill(bitCache, (byte) 0);

        List<String> toggleableList = new ArrayList<>();
        String[] doorMats = {"ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE", "CRIMSON", "WARPED", "IRON"};
        String[] fenceMats = {"ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE", "CRIMSON", "WARPED"};
        String[] buttonMats = {"ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE", "CRIMSON", "WARPED", "STONE", "POLISHED_BLACKSTONE"};
        String[] plateMats = {"ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE", "CRIMSON", "WARPED", "STONE", "POLISHED_BLACKSTONE", "LIGHT_WEIGHTED", "HEAVY_WEIGHTED"};

        for (String mat : doorMats) {
            toggleableList.add(mat + "_DOOR");
            toggleableList.add(mat + "_TRAPDOOR");
        }
        for (String mat : fenceMats) toggleableList.add(mat + "_FENCE_GATE");
        for (String mat : buttonMats) toggleableList.add(mat + "_BUTTON");
        for (String mat : plateMats) toggleableList.add(mat + "_PRESSURE_PLATE");

        toggleableList.add("LEVER");
        toggleableList.add("TRIPWIRE_HOOK");
        toggleableList.add("REDSTONE_TORCH");
        toggleableList.add("REDSTONE_WALL_TORCH");
        toggleableList.add("DAYLIGHT_DETECTOR");
        toggleableList.add("REPEATER");
        toggleableList.add("COMPARATOR");

        List<String> placeableList = new ArrayList<>();
        placeableList.add("COMPOSTER");
        placeableList.add("CHISELED_BOOKSHELF");
        placeableList.add("CAMPFIRE");
        placeableList.add("SOUL_CAMPFIRE");
        placeableList.add("LECTERN");
        placeableList.add("JUKEBOX");
        placeableList.add("CAULDRON");
        placeableList.add("WATER_CAULDRON");
        placeableList.add("LAVA_CAULDRON");
        placeableList.add("POWDER_SNOW_CAULDRON");

        List<String> VINES = new ArrayList<>();
        VINES.add("VINE");
        VINES.add("TWISTING_VINES");
        VINES.add("WEEPING_VINES");

        List<String> SCULK = new ArrayList<>();
        SCULK.add("SCULK");
        SCULK.add("SCULK_VEIN");

        List<String> CHORUS = new ArrayList<>();
        CHORUS.add("CHORUS_FLOWER");

        List<String> BAMBOO = new ArrayList<>();
        BAMBOO.add("BAMBOO");

        List<String> AMETHYST = new ArrayList<>();
        AMETHYST.add("SMALL_AMETHYST_BUD");

        for (String block : toggleableList) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= TOGGLEABLE_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : placeableList) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= PLACEABLE_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : VINES) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= VINE_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : SCULK) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= SCULK_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : CHORUS) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= CHORUS_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : BAMBOO) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= BAMBOO_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (String block : AMETHYST) {
            try {
                Material material = Material.valueOf(block);
                bitCache[material.ordinal()] |= AMETHYST_FLAG;
            } catch (IllegalArgumentException ignored) {
            }
        }

        initialized = true;
    }

    public static boolean isToggleableState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & TOGGLEABLE_FLAG) != 0;
    }

    public static boolean isPlaceableState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & PLACEABLE_FLAG) != 0;
    }

    public static boolean isSculkState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & SCULK_FLAG) != 0;
    }

    public static boolean isVineState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & VINE_FLAG) != 0;
    }

    public static boolean isChorusState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & CHORUS_FLAG) != 0;
    }

    public static boolean isAmethystState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & AMETHYST_FLAG) != 0;
    }

    public static boolean isBambooState(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & BAMBOO_FLAG) != 0;
    }

}