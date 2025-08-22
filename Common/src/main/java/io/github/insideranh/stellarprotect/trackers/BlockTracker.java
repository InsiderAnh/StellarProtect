package io.github.insideranh.stellarprotect.trackers;

import java.util.ArrayList;
import java.util.List;

public class BlockTracker {

    private static final int[] TOGGLEABLE_HASHES;
    private static final int[] PLACEABLE_HASHES;

    private static final boolean[] TOGGLEABLE_CACHE = new boolean[8192];
    private static final boolean[] PLACEABLE_CACHE = new boolean[4096];

    private static final boolean[] TOGGLEABLE_CACHED = new boolean[8192];
    private static final boolean[] PLACEABLE_CACHED = new boolean[4096];

    static {
        List<String> toggleableList = new ArrayList<>();
        List<String> placeableList = new ArrayList<>();

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

        TOGGLEABLE_HASHES = toggleableList.stream()
            .mapToInt(String::hashCode)
            .sorted()
            .toArray();

        PLACEABLE_HASHES = placeableList.stream()
            .mapToInt(String::hashCode)
            .sorted()
            .toArray();
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            isToggleableState("STONE");
            isToggleableState("OAK_DOOR");
        }
        long end = System.nanoTime();
        System.out.println("Tiempo: " + (end - start) / 1_000_000 + "ms");
    }

    private static boolean binarySearchHash(int[] sortedHashes, int hash) {
        int low = 0;
        int high = sortedHashes.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = sortedHashes[mid];

            if (midVal < hash) {
                low = mid + 1;
            } else if (midVal > hash) {
                high = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean isToggleableState(String block) {
        int hash = block.hashCode();
        int cacheIdx = hash & 8191;

        if (TOGGLEABLE_CACHED[cacheIdx]) {
            return TOGGLEABLE_CACHE[cacheIdx];
        }

        boolean result = binarySearchHash(TOGGLEABLE_HASHES, hash);

        if (!result && !block.equals(block.toUpperCase())) {
            result = binarySearchHash(TOGGLEABLE_HASHES, block.toUpperCase().hashCode());
        }

        TOGGLEABLE_CACHE[cacheIdx] = result;
        TOGGLEABLE_CACHED[cacheIdx] = true;

        return result;
    }

    public static boolean isPlaceableState(String block) {
        int hash = block.hashCode();
        int cacheIdx = hash & 4095;

        if (PLACEABLE_CACHED[cacheIdx]) {
            return PLACEABLE_CACHE[cacheIdx];
        }

        boolean result = binarySearchHash(PLACEABLE_HASHES, hash);
        if (!result && !block.equals(block.toUpperCase())) {
            result = binarySearchHash(PLACEABLE_HASHES, block.toUpperCase().hashCode());
        }

        PLACEABLE_CACHE[cacheIdx] = result;
        PLACEABLE_CACHED[cacheIdx] = true;

        return result;
    }

}