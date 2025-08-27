package io.github.insideranh.stellarprotect.blocks.adjacents;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum AdjacentType {

    UP("STICKY_PISTON", "DEAD_BUSH", "MOVING_PISTON", "WHEAT", "LEVER", "TORCH", "SOUL_TORCH",
        "STONE_PRESSURE_PLATE", "REDSTONE_TORCH", "REDSTONE", "POTATOES", "BEETROOTS", "COMPARATOR",
        "BAMBOO", "TURTLE_EGG", "CARROTS", "BEACON", "ITEM_FRAME", "CONDUIT",
        "HEAVY_WEIGHTED_PRESSURE_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE",
        "SNOW", "CACTUS", "MELON_STEM", "PISTON", "PISTON_HEAD", "LILY_PAD", "NETHER_WART",
        "BELL", "SUGAR_CANE", "NETHER_PORTAL", "REPEATER", "PUMPKIN_STEM",
        "OAK_PRESSURE_PLATE", "SPRUCE_PRESSURE_PLATE", "BIRCH_PRESSURE_PLATE", "JUNGLE_PRESSURE_PLATE",
        "ACACIA_PRESSURE_PLATE", "DARK_OAK_PRESSURE_PLATE", "CRIMSON_PRESSURE_PLATE", "WARPED_PRESSURE_PLATE",
        "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON", "ACACIA_BUTTON", "DARK_OAK_BUTTON",
        "CRIMSON_BUTTON", "WARPED_BUTTON", "STONE_BUTTON", "POLISHED_BLACKSTONE_BUTTON",
        "WHITE_CARPET", "ORANGE_CARPET", "MAGENTA_CARPET", "LIGHT_BLUE_CARPET", "YELLOW_CARPET", "LIME_CARPET",
        "PINK_CARPET", "GRAY_CARPET", "LIGHT_GRAY_CARPET", "CYAN_CARPET", "PURPLE_CARPET", "BLUE_CARPET",
        "BROWN_CARPET", "GREEN_CARPET", "RED_CARPET", "BLACK_CARPET",
        "FLOWER_POT", "POTTED_OAK_SAPLING", "POTTED_SPRUCE_SAPLING", "POTTED_BIRCH_SAPLING", "POTTED_JUNGLE_SAPLING",
        "POTTED_ACACIA_SAPLING", "POTTED_DARK_OAK_SAPLING", "POTTED_FERN", "POTTED_DANDELION", "POTTED_POPPY",
        "POTTED_BLUE_ORCHID", "POTTED_ALLIUM", "POTTED_AZURE_BLUET", "POTTED_RED_TULIP", "POTTED_ORANGE_TULIP",
        "POTTED_WHITE_TULIP", "POTTED_PINK_TULIP", "POTTED_OXEYE_DAISY", "POTTED_CORNFLOWER", "POTTED_LILY_OF_THE_VALLEY",
        "POTTED_WITHER_ROSE", "POTTED_RED_MUSHROOM", "POTTED_BROWN_MUSHROOM", "POTTED_DEAD_BUSH", "POTTED_CACTUS",
        "POTTED_BAMBOO", "POTTED_CRIMSON_FUNGUS", "POTTED_WARPED_FUNGUS", "POTTED_CRIMSON_ROOTS", "POTTED_WARPED_ROOTS",
        "OAK_DOOR", "SPRUCE_DOOR", "BIRCH_DOOR", "JUNGLE_DOOR", "ACACIA_DOOR", "DARK_OAK_DOOR", "CRIMSON_DOOR", "WARPED_DOOR",
        "RAIL", "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL",
        "OAK_SAPLING", "SPRUCE_SAPLING", "BIRCH_SAPLING", "JUNGLE_SAPLING", "ACACIA_SAPLING", "DARK_OAK_SAPLING", "CRIMSON_FUNGUS", "WARPED_FUNGUS",
        "WHITE_BANNER", "ORANGE_BANNER", "MAGENTA_BANNER", "LIGHT_BLUE_BANNER", "YELLOW_BANNER", "LIME_BANNER", "PINK_BANNER", "GRAY_BANNER",
        "LIGHT_GRAY_BANNER", "CYAN_BANNER", "PURPLE_BANNER", "BLUE_BANNER", "BROWN_BANNER", "GREEN_BANNER", "RED_BANNER", "BLACK_BANNER",
        "GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "DEAD_BUSH", "SEAGRASS", "TALL_SEAGRASS",
        "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET", "RED_TULIP", "ORANGE_TULIP",
        "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY", "CORNFLOWER", "LILY_OF_THE_VALLEY", "WITHER_ROSE",
        "SUNFLOWER", "LILAC", "ROSE_BUSH", "PEONY", "RED_MUSHROOM", "BROWN_MUSHROOM", "CRIMSON_ROOTS", "WARPED_ROOTS",
        "OAK_SIGN", "SPRUCE_SIGN", "BIRCH_SIGN", "JUNGLE_SIGN", "ACACIA_SIGN", "DARK_OAK_SIGN", "CRIMSON_SIGN", "WARPED_SIGN",

        // <1.13 materials
        "REDSTONE_WIRE", "SNOW_LAYER", "STONE_PLATE", "WOOD_PLATE",
        "WOOD_BUTTON", "WOODEN_DOOR", "WOODEN_SLAB", "WOOD_STAIRS", "SIGN_POST",
        "WALL_SIGN", "LONG_GRASS", "HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2", "DOUBLE_PLANT"
    ),
    DOWN,
    SIDE(
        "WALL_TORCH", "REDSTONE_WALL_TORCH", "TORCH", "REDSTONE_TORCH", "REDSTONE_TORCH_OFF", "REDSTONE_TORCH_ON",
        "VINE", "COCOA", "TRIPWIRE", "TRIPWIRE_HOOK", "ITEM_FRAME", "PAINTING", "LADER", "LEVER", "SOUL_TORCH", "SOUL_WALL_TORCH",
        "WHITE_BANNER", "ORANGE_BANNER", "MAGENTA_BANNER", "LIGHT_BLUE_BANNER", "YELLOW_BANNER", "LIME_BANNER", "PINK_BANNER", "GRAY_BANNER",
        "LIGHT_GRAY_BANNER", "CYAN_BANNER", "PURPLE_BANNER", "BLUE_BANNER", "BROWN_BANNER", "GREEN_BANNER", "RED_BANNER", "BLACK_BANNER",
        "WHITE_WALL_BANNER", "ORANGE_WALL_BANNER", "MAGENTA_WALL_BANNER", "LIGHT_WALL_BLUE_WALL_BANNER", "YELLOW_WALL_BANNER", "LIME_WALL_BANNER", "PINK_WALL_BANNER",
        "GRAY_WALL_BANNER", "LIGHT_WALL_GRAY_WALL_BANNER", "CYAN_WALL_BANNER", "PURPLE_WALL_BANNER", "BLUE_WALL_BANNER", "BROWN_WALL_BANNER",
        "GREEN_WALL_BANNER", "RED_WALL_BANNER", "BLACK_WALL_BANNER",
        "OAK_SIGN", "SPRUCE_SIGN", "BIRCH_SIGN", "JUNGLE_SIGN", "ACACIA_SIGN", "DARK_OAK_SIGN", "CRIMSON_SIGN", "WARPED_SIGN",
        "OAK_WALL_SIGN", "SPRUCE_WALL_SIGN", "BIRCH_WALL_SIGN", "JUNGLE_WALL_SIGN", "ACACIA_WALL_SIGN", "DARK_WALL_OAK_WALL_SIGN", "CRIMSON_WALL_SIGN", "WARPED_WALL_SIGN",
        "OAK_BUTTON", "SPRUCE_BUTTON", "BIRCH_BUTTON", "JUNGLE_BUTTON", "ACACIA_BUTTON", "DARK_OAK_BUTTON",
        "CRIMSON_BUTTON", "WARPED_BUTTON", "STONE_BUTTON", "POLISHED_BLACKSTONE_BUTTON"
    );

    private static final byte[] bitCache = new byte[Material.values().length];
    private static final byte UP_FLAG = 1;
    private static final byte DOWN_FLAG = 2;
    private static final byte SIDE_FLAG = 3;
    private static boolean initialized = false;
    private final List<String> blocks = new ArrayList<>();

    AdjacentType(String... blocks) {
        this.blocks.addAll(Arrays.asList(blocks));
    }

    public static void initializeCache() {
        if (initialized) return;
        Arrays.fill(bitCache, (byte) 0);
        for (AdjacentType type : values()) {
            byte flag = getFlag(type);

            for (String block : type.blocks) {
                try {
                    Material material = Material.valueOf(block);
                    bitCache[material.ordinal()] |= flag;
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        initialized = true;
    }

    public static boolean isUp(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & UP_FLAG) != 0;
    }

    public static boolean isSide(Material material) {
        int ordinal = material.ordinal();
        return (bitCache[ordinal] & SIDE_FLAG) != 0;
    }

    private static byte getFlag(AdjacentType type) {
        switch (type) {
            case UP:
                return UP_FLAG;
            case DOWN:
                return DOWN_FLAG;
            case SIDE:
                return SIDE_FLAG;
            default:
                return 0;
        }
    }

}