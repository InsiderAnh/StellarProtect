package io.github.insideranh.stellarprotect.enums;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.config.WorldConfigType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public enum ActionType {

    BLOCK_BREAK(0),
    BLOCK_PLACE(1),
    BLOCK_USE(2),

    DROP_ITEM(3),
    PICKUP_ITEM(4),

    FURNACE_EXTRACT(5),
    FURNACE_PLACE(6),

    CRAFT(7),
    ENCHANT(21),

    CROP_GROW(8),

    KILL_ENTITY(9),
    INTERACT(10),
    TAME(11),
    BREED(12),

    CHAT(13, false),
    COMMAND(14, false),

    BUCKET_EMPTY(15),
    BUCKET_FILL(16),
    INVENTORY_TRANSACTION(17),
    USE(18),
    SESSION(19),
    SIGN_CHANGE(20),
    DEATH(23),

    MOUNT(24),
    RAID(25),
    HANGING(26),
    SMITH(27),
    BREWING(28),
    REPAIR(29),
    SHOOT(30),
    TOTEM(31),

    TELEPORT(32),
    CONSUME(33),
    GAME_MODE(34),

    XP(35),
    MONEY(36),

    DECORATIVE_ITEM_CHANGE(37),

    PLACE_ITEM(38),
    REMOVE_ITEM(39),

    TREE_GROW(40),

    SHOP_GUI(80),

    // Nexo
    FURNITURE_BREAK(81, false),
    FURNITURE_PLACE(82, false),

    X_KIT_EVENT(83, false);

    private static final ActionType[] ID_TO_ACTION_CACHE;
    private static final Map<String, ActionType> NAME_TO_ACTION_CACHE = new HashMap<>();

    private static final Map<String, String> WORLD_LOWER_CACHE = new ConcurrentHashMap<>();

    static {
        int maxId = Arrays.stream(ActionType.values())
            .mapToInt(ActionType::getId)
            .max()
            .orElse(0);

        ID_TO_ACTION_CACHE = new ActionType[maxId + 1];

        for (ActionType actionType : ActionType.values()) {
            ID_TO_ACTION_CACHE[actionType.getId()] = actionType;
            NAME_TO_ACTION_CACHE.put(actionType.name().toLowerCase(), actionType);
        }
    }

    private final int id;
    private final HashMap<String, WorldConfigType> worldTypes = new HashMap<>();

    private final Set<String> worlds = new HashSet<>();
    private final Set<String> disabledTypes = new HashSet<>();

    @Setter
    private boolean hasAllWorlds = false;

    @Setter
    private boolean enabled = true;
    private boolean parseMinecraftData = true;

    ActionType(int id) {
        this.id = id;
    }

    ActionType(int id, boolean parseMinecraftData) {
        this.id = id;
        this.parseMinecraftData = parseMinecraftData;
    }

    private static String getLowerCaseWorld(String world) {
        if (world == null) return null;
        return WORLD_LOWER_CACHE.computeIfAbsent(world, String::toLowerCase);
    }

    public static ActionType getById(int id) {
        if (id >= 0 && id < ID_TO_ACTION_CACHE.length) {
            return ID_TO_ACTION_CACHE[id];
        }
        return null;
    }

    public static ActionType getByName(String name) {
        return NAME_TO_ACTION_CACHE.get(name.toLowerCase());
    }

    public static List<String> getAllNamesNoPrefix(@Nullable String filter) {
        List<String> names = new ArrayList<>();
        for (ActionType actionType : ActionType.values()) {
            String name = actionType.name().toLowerCase();
            if (filter == null || name.contains(filter)) {
                names.add(name);
            }
        }
        return names;
    }

    public static List<String> getAllNames(@Nullable String filter) {
        List<String> names = new ArrayList<>();
        for (ActionType actionType : ActionType.values()) {
            String name = "a:" + actionType.name().toLowerCase();
            if (filter == null || name.contains(filter)) {
                names.add(name);
            }
        }
        return names;
    }

    public boolean shouldSkipLog(String world, String type) {
        if (!enabled) {
            return true;
        }

        String typeLower = type.toLowerCase();

        WorldConfigType worldConfig = worldTypes.get(world);
        if (worldConfig != null) {
            return !worldConfig.isEnabled() || worldConfig.getDisabledTypes().contains(typeLower);
        }

        if (disabledTypes.contains(typeLower)) {
            return true;
        }

        if (hasAllWorlds) {
            return false;
        }

        String worldLower = getLowerCaseWorld(world);
        return worldLower != null && !worlds.contains(worldLower);
    }

    public boolean shouldSkipLogStart(@Nullable String world, String type) {
        if (!enabled) {
            return true;
        }

        String typeLower = type.toLowerCase();

        WorldConfigType worldConfig = worldTypes.get(world);
        if (worldConfig != null) {
            if (!worldConfig.isEnabled()) {
                return true;
            }
            for (String disabledType : worldConfig.getDisabledTypes()) {
                if (typeLower.startsWith(disabledType)) {
                    return true;
                }
            }
            return false;
        }

        for (String disabledType : disabledTypes) {
            if (typeLower.startsWith(disabledType)) {
                return true;
            }
        }

        if (hasAllWorlds) {
            return false;
        }

        if (world == null) {
            return true;
        }

        String worldLower = getLowerCaseWorld(world);
        return !worlds.contains(worldLower);
    }

}