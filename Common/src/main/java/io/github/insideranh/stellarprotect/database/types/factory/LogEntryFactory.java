package io.github.insideranh.stellarprotect.database.types.factory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerEconomyEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerXPEntry;
import io.github.insideranh.stellarprotect.database.entries.entity.EntityResurrectEntry;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerShopGUIEntry;
import io.github.insideranh.stellarprotect.database.entries.players.*;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerChatEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import lombok.Getter;
import org.bson.Document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogEntryFactory {

    @Getter
    public enum ActionLogType {
        BLOCK_BREAK(0, PlayerBlockLogEntry.class),
        BLOCK_PLACE(1, PlayerBlockLogEntry.class),
        BLOCK_INTERACT(10, PlayerBlockLogEntry.class),
        BLOCK_BURN(15, PlayerBlockLogEntry.class),
        BLOCK_EXPLODE(16, PlayerBlockLogEntry.class),

        ITEM_DROP(3, PlayerItemLogEntry.class),
        ITEM_PICKUP(4, PlayerItemLogEntry.class),
        ITEM_CRAFT(5, PlayerItemLogEntry.class),
        ITEM_ENCHANT(7, PlayerItemLogEntry.class),
        ITEM_CONSUME(21, PlayerItemLogEntry.class),
        ITEM_REPAIR(22, PlayerItemLogEntry.class),
        ITEM_BOOK_EDIT(33, PlayerItemLogEntry.class),

        PLAYER_TRANSACTION(17, PlayerTransactionEntry.class),
        PLAYER_USE(18, PlayerUseEntry.class),
        PLAYER_SESSION(19, PlayerSessionEntry.class),
        PLAYER_SIGN_CHANGE(20, PlayerSignChangeEntry.class),
        CROP_GROW(8, CropGrowLogEntry.class),
        PLAYER_KILL(9, PlayerKillLogEntry.class),
        PLAYER_DEATH(23, PlayerDeathEntry.class),
        PLAYER_TAME(11, PlayerTameEntry.class),
        PLAYER_BREED(12, PlayerBreedEntry.class),
        PLAYER_CHAT(13, PlayerChatEntry.class),
        PLAYER_COMMAND(14, PlayerCommandEntry.class),
        PLAYER_MOUNT(24, PlayerMountEntry.class),
        PLAYER_HANGING(26, PlayerHangingEntry.class),
        PLAYER_SHOOT(30, PlayerShootEntry.class),
        ENTITY_RESURRECT(31, EntityResurrectEntry.class),
        PLAYER_TELEPORT(32, PlayerTeleportLogEntry.class),
        PLAYER_GAMEMODE(34, PlayerGameModeLogEntry.class),
        PLAYER_XP(35, PlayerXPEntry.class),
        PLAYER_ECONOMY(36, PlayerEconomyEntry.class),
        PLAYER_SHOP_GUI(80, PlayerShopGUIEntry.class);

        private final int actionId;
        private final Class<? extends LogEntry> entryClass;

        ActionLogType(int actionId, Class<? extends LogEntry> entryClass) {
            this.actionId = actionId;
            this.entryClass = entryClass;
        }

    }

    private static final Map<Integer, ActionLogType> ACTION_TYPE_MAP =
        Arrays.stream(ActionLogType.values())
            .collect(Collectors.toMap(ActionLogType::getActionId, Function.identity()));

    public static LogEntry fromDocument(Document document) {
        int type = document.getInteger("action_type");
        JsonObject extra = parseExtraJson(document.getString("extra_json"));

        return createLogEntry(ACTION_TYPE_MAP.get(type), document, extra);
    }

    public static LogEntry fromDatabase(ResultSet resultSet) throws SQLException {
        int type = resultSet.getInt("action_type");
        JsonObject extra = parseExtraJson(resultSet.getString("extra_json"));

        return createLogEntry(ACTION_TYPE_MAP.get(type), resultSet, extra);
    }

    private static LogEntry createLogEntry(ActionLogType actionLogType, Object source, JsonObject extra) {
        if (actionLogType == null) {
            return source instanceof Document ? new LogEntry((Document) source) : new LogEntry((ResultSet) source);
        }

        try {
            Class<? extends LogEntry> entryClass = actionLogType.getEntryClass();
            if (source instanceof Document) {
                Document document = (Document) source;
                return entryClass.getConstructor(Document.class, JsonObject.class).newInstance(document, extra);
            } else {
                ResultSet resultSet = (ResultSet) source;
                return entryClass.getConstructor(ResultSet.class, JsonObject.class).newInstance(resultSet, extra);
            }
        } catch (Exception e) {
            return source instanceof Document ? new LogEntry((Document) source) : new LogEntry((ResultSet) source);
        }
    }

    private static JsonObject parseExtraJson(String extraJson) {
        if (extraJson == null || extraJson.trim().isEmpty()) {
            return new JsonObject();
        }

        try {
            return new JsonParser().parse(extraJson).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

}