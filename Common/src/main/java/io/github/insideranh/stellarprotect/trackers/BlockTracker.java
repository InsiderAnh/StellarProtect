package io.github.insideranh.stellarprotect.trackers;

import io.github.insideranh.stellarprotect.maps.StringByteMap;
import io.github.insideranh.stellarprotect.maps.StringHashSet;

public class BlockTracker {

    private static final StringHashSet TOGGLEABLE_BLOCKS = new StringHashSet(64);
    private static final StringHashSet PLACEABLE_BLOCKS = new StringHashSet(32);
    private static final StringHashSet INTERACTABLE_BLOCKS = new StringHashSet(128);

    private static final StringByteMap UNIFIED_CACHE = new StringByteMap(1024);

    private static final byte TOGGLEABLE_FLAG = 1;
    private static final byte PLACEABLE_FLAG = 2;
    private static final byte INTERACTABLE_FLAG = 4;
    private static final byte CHECKED_FLAG = 8;

    private static final String[] DOOR_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED", "IRON"
    };

    private static final String[] TRAPDOOR_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED", "IRON"
    };

    private static final String[] FENCE_GATE_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED"
    };

    private static final String[] BUTTON_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED", "STONE", "POLISHED_BLACKSTONE"
    };

    private static final String[] PRESSURE_PLATE_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED", "STONE", "POLISHED_BLACKSTONE", "LIGHT_WEIGHTED", "HEAVY_WEIGHTED"
    };

    private static final String[] CHEST_MATERIALS = {
        "ACACIA", "BIRCH", "DARK_OAK", "JUNGLE", "OAK", "SPRUCE", "CHERRY", "BAMBOO", "MANGROVE",
        "CRIMSON", "WARPED"
    };

    private static final String[] SHULKER_COLORS = {
        "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
        "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    static {
        UNIFIED_CACHE.defaultReturnValue((byte) 0);

        for (String material : DOOR_MATERIALS) {
            TOGGLEABLE_BLOCKS.add(material + "_DOOR");
        }

        for (String material : TRAPDOOR_MATERIALS) {
            TOGGLEABLE_BLOCKS.add(material + "_TRAPDOOR");
        }

        for (String material : FENCE_GATE_MATERIALS) {
            TOGGLEABLE_BLOCKS.add(material + "_FENCE_GATE");
        }

        for (String material : BUTTON_MATERIALS) {
            TOGGLEABLE_BLOCKS.add(material + "_BUTTON");
        }

        for (String material : PRESSURE_PLATE_MATERIALS) {
            TOGGLEABLE_BLOCKS.add(material + "_PRESSURE_PLATE");
        }

        String[] specialToggleables = {
            "LEVER", "TRIPWIRE_HOOK", "REDSTONE_TORCH", "REDSTONE_WALL_TORCH",
            "DAYLIGHT_DETECTOR", "REPEATER", "COMPARATOR"
        };

        TOGGLEABLE_BLOCKS.addAll(specialToggleables);

        String[] placeableArray = {"COMPOSTER", "CHISELED_BOOKSHELF", "CAMPFIRE",
            "SOUL_CAMPFIRE", "LECTERN", "JUKEBOX", "CAULDRON", "WATER_CAULDRON",
            "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON"};
        PLACEABLE_BLOCKS.addAll(placeableArray);

        String[] interactableArray = {"STONECUTTER", "DISPENSER", "DROPPER", "HOPPER",
            "BREWING_STAND", "ENCHANTING_TABLE", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "BEACON",
            "BARREL", "SMITHING_TABLE", "CRAFTING_TABLE", "LOOM", "SMOKER",
            "FLETCHING_TABLE", "GRINDSTONE", "CARTOGRAPHY_TABLE", "NOTE_BLOCK",
            "CRAFTER", "BELL", "COMPOSTER", "CHISELED_BOOKSHELF", "CAMPFIRE",
            "SOUL_CAMPFIRE", "LECTERN", "JUKEBOX", "CAULDRON", "FURNACE", "BLAST_FURNACE",
            "WORKBENCH", "ENCHANTMENT_TABLE", "DIODE", "REDSTONE_COMPARATOR"};

        INTERACTABLE_BLOCKS.addAll(interactableArray);

        for (String material : CHEST_MATERIALS) {
            INTERACTABLE_BLOCKS.add(material + "_CHEST");
        }
        INTERACTABLE_BLOCKS.add("CHEST");
        INTERACTABLE_BLOCKS.add("TRAPPED_CHEST");
        INTERACTABLE_BLOCKS.add("ENDER_CHEST");

        for (String color : SHULKER_COLORS) {
            INTERACTABLE_BLOCKS.add(color + "_SHULKER_BOX");
        }
        INTERACTABLE_BLOCKS.add("SHULKER_BOX");

        for (String color : SHULKER_COLORS) {
            INTERACTABLE_BLOCKS.add(color + "_BED");
        }

        INTERACTABLE_BLOCKS.add("COMMAND_BLOCK");
        INTERACTABLE_BLOCKS.add("CHAIN_COMMAND_BLOCK");
        INTERACTABLE_BLOCKS.add("REPEATING_COMMAND_BLOCK");

        INTERACTABLE_BLOCKS.addAll(TOGGLEABLE_BLOCKS);
        INTERACTABLE_BLOCKS.addAll(PLACEABLE_BLOCKS);
    }

    public static boolean isToggleableState(String block) {
        byte cached = UNIFIED_CACHE.getByte(block);
        if ((cached & CHECKED_FLAG) != 0) {
            return (cached & TOGGLEABLE_FLAG) != 0;
        }

        boolean result = TOGGLEABLE_BLOCKS.contains(block);
        if (!result && !block.equals(block.toUpperCase())) {
            result = TOGGLEABLE_BLOCKS.contains(block.toUpperCase());
        }

        byte flags = (byte) (CHECKED_FLAG | (result ? TOGGLEABLE_FLAG : 0));
        UNIFIED_CACHE.put(block, flags);
        return result;
    }

    public static boolean isPlaceableState(String block) {
        byte cached = UNIFIED_CACHE.getByte(block);
        if ((cached & CHECKED_FLAG) != 0) {
            return (cached & PLACEABLE_FLAG) != 0;
        }

        boolean result = PLACEABLE_BLOCKS.contains(block);
        if (!result && !block.equals(block.toUpperCase())) {
            result = PLACEABLE_BLOCKS.contains(block.toUpperCase());
        }

        byte flags = (byte) (CHECKED_FLAG | (result ? PLACEABLE_FLAG : 0));
        UNIFIED_CACHE.put(block, flags);
        return result;
    }

    public static boolean isInteractable(String block) {
        byte cached = UNIFIED_CACHE.getByte(block);
        if ((cached & CHECKED_FLAG) != 0) {
            return (cached & INTERACTABLE_FLAG) != 0;
        }

        boolean result = INTERACTABLE_BLOCKS.contains(block);
        if (!result && !block.equals(block.toUpperCase())) {
            result = INTERACTABLE_BLOCKS.contains(block.toUpperCase());
        }

        byte flags = (byte) (CHECKED_FLAG | (result ? INTERACTABLE_FLAG : 0));
        UNIFIED_CACHE.put(block, flags);
        return result;
    }

}