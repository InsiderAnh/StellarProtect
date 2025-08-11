package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public enum ActionCategory {

    BLOCK_ACTIONS(0, "block_actions", new ActionType[]{
        ActionType.BLOCK_BREAK, ActionType.BLOCK_PLACE, ActionType.BLOCK_USE
    }),
    ITEM_ACTIONS(1, "item_actions", new ActionType[]{
        ActionType.DROP_ITEM, ActionType.PICKUP_ITEM, ActionType.FURNACE_EXTRACT, ActionType.FURNACE_PLACE,
        ActionType.ENCHANT, ActionType.SMITH,
        // NOT IMPLEMENTED
        ActionType.REPAIR, ActionType.BREWING
    }),
    ENTITY_ACTIONS(2, "entity_actions", new ActionType[]{
        ActionType.KILL_ENTITY, ActionType.DEATH, ActionType.INTERACT, ActionType.USE, ActionType.TAME, ActionType.BREED,
        // NOT IMPLEMENTED
        ActionType.MOUNT, ActionType.RAID
    }),
    SYSTEM_ACTIONS(3, "system_actions", new ActionType[]{
        ActionType.CROP_GROW, ActionType.CRAFT
    }),
    COMMUNICATION_ACTIONS(4, "communication_actions", new ActionType[]{
        ActionType.CHAT, ActionType.COMMAND
    }),
    FLUID_ACTIONS(5, "fluid_actions", new ActionType[]{
        ActionType.BUCKET_EMPTY, ActionType.BUCKET_FILL
    }),
    INVENTORY_ACTIONS(6, "inventory_actions", new ActionType[]{
        ActionType.INVENTORY_TRANSACTION
    }),
    SESSION_ACTIONS(7, "session_actions", new ActionType[]{
        ActionType.SESSION
    }),
    SIGN_ACTIONS(8, "sign_actions", new ActionType[]{
        ActionType.SIGN_CHANGE
    }),
    PLAYER_ACTIONS(9, "player_actions", new ActionType[]{
        ActionType.SHOOT, ActionType.TOTEM, ActionType.CONSUME, ActionType.GAME_MODE,
        ActionType.TELEPORT, ActionType.XP, ActionType.MONEY
    }),
    UNKNOWN_ACTIONS(50, "unknown_actions", ActionType.values());

    private final int id;
    private final String tableName;
    private final ActionType[] actions;
    private final Set<ActionType> actionSet;

    ActionCategory(int id, String tableName, ActionType[] actions) {
        this.id = id;
        this.tableName = tableName;
        this.actions = actions;
        this.actionSet = new HashSet<>();
        this.actionSet.addAll(Arrays.asList(actions));
    }

    public static Set<ActionCategory> fromActionTypes(List<ActionType> actionTypes) {
        Set<ActionCategory> categories = new HashSet<>();
        for (ActionCategory category : values()) {
            for (ActionType action : category.actionSet) {
                if (actionTypes.contains(action)) {
                    categories.add(category);
                }
            }
        }
        return categories;
    }

    public static ActionCategory fromActionTypes(ActionType actionType) {
        for (ActionCategory category : values()) {
            if (category.actionSet.contains(actionType)) {
                return category;
            }
        }
        return SYSTEM_ACTIONS;
    }

}
