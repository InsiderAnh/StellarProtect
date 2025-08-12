package io.github.insideranh.stellarprotect.enums;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.config.WorldConfigType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

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

    CHAT(13),
    COMMAND(14),

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

    SHOP_GUI(80);

    private static final ActionType[] ID_TO_ACTION_CACHE;
    private static final Map<String, ActionType> NAME_TO_ACTION_CACHE = new HashMap<>();

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
    private final HashSet<String> worlds = new HashSet<>();
    private final HashSet<String> disabledTypes = new HashSet<>();
    @Setter
    private boolean enabled = true;

    ActionType(int id) {
        this.id = id;
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
        List<String> names = new LinkedList<>();
        for (ActionType actionType : ActionType.values()) {
            if (filter == null || actionType.name().toLowerCase().contains(filter)) {
                names.add(actionType.name().toLowerCase());
            }
        }
        return names;
    }

    public static List<String> getAllNames(@Nullable String filter) {
        List<String> names = new LinkedList<>();
        for (ActionType actionType : ActionType.values()) {
            if (filter == null || actionType.name().toLowerCase().contains(filter)) {
                names.add("a:" + actionType.name().toLowerCase());
            }
        }
        return names;
    }

    public boolean shouldSkipLog(String world, String type) {
        WorldConfigType worldConfigType = worldTypes.get(world);
        if (worldConfigType != null) {
            return !worldConfigType.isEnabled() || worldConfigType.getDisabledTypes().contains(type.toLowerCase());
        }
        return !enabled || disabledTypes.contains(type.toLowerCase()) || (!worlds.contains("all") && !worlds.contains(world.toLowerCase()));
    }

    public boolean shouldSkipLogStart(@Nullable String world, String type) {
        WorldConfigType worldConfigType = worldTypes.get(world);
        if (worldConfigType != null) {
            for (String disabledType : worldConfigType.getDisabledTypes()) {
                if (disabledType.startsWith(type.toLowerCase())) {
                    return true;
                }
            }
            return !worldConfigType.isEnabled();
        }

        for (String disabledType : disabledTypes) {
            if (disabledType.startsWith(type.toLowerCase())) {
                return true;
            }
        }
        return !enabled || (!worlds.contains("all") && world != null && !worlds.contains(world.toLowerCase()));
    }

}