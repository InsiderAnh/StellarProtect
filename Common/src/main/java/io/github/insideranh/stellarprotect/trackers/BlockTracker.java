package io.github.insideranh.stellarprotect.trackers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockTracker {

    private static final String[] TOGGLEABLE_SUFFIXES = {
        "DOOR", "TRAPDOOR", "FENCE_GATE", "BUTTON", "PRESSURE_PLATE", "LEVER"
    };

    private static final String[] ENDWITH_SUFFIXES = {
        "CHEST", "SHULKER_BOX", "BED", "DOOR", "COMMAND_BLOCK", "FURNACE"
    };

    private static final char[][] TOGGLEABLE_CHARS = new char[6][];
    private static final int[] TOGGLEABLE_LENGTHS = new int[6];
    private static final char[][] ENDWITH_CHARS = new char[6][];
    private static final int[] ENDWITH_LENGTHS = new int[6];

    private static final Map<String, Boolean> INTERACTABLE_MAP = new HashMap<>(35, 0.75f);
    private static final Map<String, Boolean> LEGACY_MAP = new HashMap<>(6, 0.75f);
    private static final Map<String, Boolean> PLACEABLE_MAP = new HashMap<>(10, 0.75f);

    private static final Map<String, Byte> CACHE = new ConcurrentHashMap<>(512, 0.75f);
    private static final byte TOGGLEABLE_FLAG = 1;
    private static final byte PLACEABLE_FLAG = 2;
    private static final byte INTERACTABLE_FLAG = 4;
    private static final byte NOT_FOUND = 8;

    static {
        String[] interactable = {"STONECUTTER", "DISPENSER", "DROPPER", "HOPPER",
            "BREWING_STAND", "ENCHANTING_TABLE", "ANVIL", "BEACON",
            "BARREL", "SMITHING_TABLE", "CRAFTING_TABLE", "LOOM",
            "SMOKER", "FLETCHING_TABLE", "GRINDSTONE", "CARTOGRAPHY_TABLE",
            "NOTE_BLOCK", "CRAFTER", "BELL", "COMPOSTER", "CHISELED_BOOKSHELF",
            "CAMPFIRE", "SOUL_CAMPFIRE", "LECTERN", "JUKEBOX", "CAULDRON"};

        String[] legacyBlocks = {"WORKBENCH", "ENCHANTMENT_TABLE", "DIODE", "REDSTONE_COMPARATOR"};

        String[] placeableBlocks = {"COMPOSTER", "CHISELED_BOOKSHELF", "CAMPFIRE",
            "SOUL_CAMPFIRE", "LECTERN", "JUKEBOX", "CAULDRON"};

        for (String block : interactable) {
            INTERACTABLE_MAP.put(block, true);
        }
        for (String block : legacyBlocks) {
            LEGACY_MAP.put(block, true);
        }
        for (String block : placeableBlocks) {
            PLACEABLE_MAP.put(block, true);
        }

        for (int i = 0; i < TOGGLEABLE_SUFFIXES.length; i++) {
            TOGGLEABLE_CHARS[i] = TOGGLEABLE_SUFFIXES[i].toCharArray();
            TOGGLEABLE_LENGTHS[i] = TOGGLEABLE_CHARS[i].length;
        }
        for (int i = 0; i < ENDWITH_SUFFIXES.length; i++) {
            ENDWITH_CHARS[i] = ENDWITH_SUFFIXES[i].toCharArray();
            ENDWITH_LENGTHS[i] = ENDWITH_CHARS[i].length;
        }
    }

    private static boolean fastEndsWith(String str, char[] suffix, int suffixLen) {
        int strLen = str.length();
        if (strLen < suffixLen) return false;

        for (int i = 0; i < suffixLen; i++) {
            if (str.charAt(strLen - suffixLen + i) != suffix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String fastUpperCase(String str) {
        char[] chars = null;
        int len = str.length();

        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (chars == null) {
                    chars = str.toCharArray();
                }
                chars[i] = (char) (c - 32);
            }
        }
        return chars == null ? str : new String(chars);
    }

    private static boolean contains(String str, String sub) {
        return str.contains(sub);
    }

    public static boolean isToggleableState(String block) {
        Byte cached = CACHE.get(block);
        if (cached != null) {
            return (cached & TOGGLEABLE_FLAG) != 0;
        }

        for (int i = 0; i < TOGGLEABLE_CHARS.length; i++) {
            if (fastEndsWith(block, TOGGLEABLE_CHARS[i], TOGGLEABLE_LENGTHS[i])) {
                CACHE.put(block, TOGGLEABLE_FLAG);
                return true;
            }
        }

        CACHE.put(block, NOT_FOUND);
        return false;
    }

    public static boolean isPlaceableState(String block) {
        Byte cached = CACHE.get(block);
        if (cached != null) {
            return (cached & PLACEABLE_FLAG) != 0;
        }

        boolean result = PLACEABLE_MAP.containsKey(block) ||
            PLACEABLE_MAP.containsKey(fastUpperCase(block));

        CACHE.put(block, result ? PLACEABLE_FLAG : NOT_FOUND);
        return result;
    }

    public static boolean isInteractable(String block) {
        Byte cached = CACHE.get(block);
        if (cached != null) {
            return (cached & INTERACTABLE_FLAG) != 0;
        }

        if (INTERACTABLE_MAP.containsKey(block) || LEGACY_MAP.containsKey(block)) {
            CACHE.put(block, INTERACTABLE_FLAG);
            return true;
        }

        String upperBlock = fastUpperCase(block);
        if (INTERACTABLE_MAP.containsKey(upperBlock) || LEGACY_MAP.containsKey(upperBlock)) {
            CACHE.put(block, INTERACTABLE_FLAG);
            return true;
        }

        for (String key : INTERACTABLE_MAP.keySet()) {
            if (contains(upperBlock, key)) {
                CACHE.put(block, INTERACTABLE_FLAG);
                return true;
            }
        }

        for (String key : LEGACY_MAP.keySet()) {
            if (contains(upperBlock, key)) {
                CACHE.put(block, INTERACTABLE_FLAG);
                return true;
            }
        }

        for (int i = 0; i < TOGGLEABLE_CHARS.length; i++) {
            if (fastEndsWith(upperBlock, TOGGLEABLE_CHARS[i], TOGGLEABLE_LENGTHS[i])) {
                CACHE.put(block, INTERACTABLE_FLAG);
                return true;
            }
        }

        for (int i = 0; i < ENDWITH_CHARS.length; i++) {
            if (fastEndsWith(upperBlock, ENDWITH_CHARS[i], ENDWITH_LENGTHS[i])) {
                CACHE.put(block, INTERACTABLE_FLAG);
                return true;
            }
        }

        CACHE.put(block, NOT_FOUND);
        return false;
    }

}