package io.github.insideranh.stellarprotect.database.types.factory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerEconomyEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerXPEntry;
import io.github.insideranh.stellarprotect.database.entries.entity.EntityResurrectEntry;
import io.github.insideranh.stellarprotect.database.entries.players.*;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerChatEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import org.bson.Document;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LogEntryFactory {

    public static LogEntry fromDocument(Document document) {
        int type = document.getInteger("action_type");

        String extraJson = document.getString("extra_json");
        JsonObject extra;
        if (extraJson != null && !extraJson.isEmpty()) {
            extra = new JsonParser().parse(extraJson).getAsJsonObject();
        } else {
            extra = new JsonObject();
        }

        switch (type) {
            case 0:
            case 1:
            case 10:
            case 15:
            case 16:
                return new PlayerBlockLogEntry(document, extra);
            case 17:
                return new PlayerTransactionEntry(document, extra);
            case 18:
                return new PlayerUseEntry(document, extra);
            case 19:
                return new PlayerSessionEntry(document, extra);
            case 20:
                return new PlayerSignChangeEntry(document, extra);
            case 3:
            case 4:
            case 5:
            case 7:
            case 21:
            case 22:
            case 33:
                return new PlayerItemLogEntry(document, extra);
            case 8:
                return new CropGrowLogEntry(document, extra);
            case 9:
                return new PlayerKillLogEntry(document, extra);
            case 23:
                return new PlayerDeathEntry(document, extra);
            case 11:
                return new PlayerTameEntry(document, extra);
            case 12:
                return new PlayerBreedEntry(document, extra);
            case 13:
                return new PlayerChatEntry(document, extra);
            case 14:
                return new PlayerCommandEntry(document, extra);
            case 24:
                return new PlayerMountEntry(document, extra);
            case 26:
                return new PlayerHangingEntry(document, extra);
            case 30:
                return new PlayerShootEntry(document, extra);
            case 31:
                return new EntityResurrectEntry(document, extra);
            case 32:
                return new PlayerTeleportLogEntry(document, extra);
            case 34:
                return new PlayerGameModeLogEntry(document, extra);
            case 35:
                return new PlayerXPEntry(document, extra);
            case 36:
                return new PlayerEconomyEntry(document, extra);
            default:
                return new LogEntry(document);
        }
    }

    public static LogEntry fromDatabase(ResultSet resultSet) throws SQLException {
        int type = resultSet.getInt("action_type");

        String extraJson = resultSet.getString("extra_json");
        JsonObject extra;
        if (extraJson != null && !extraJson.isEmpty()) {
            extra = new JsonParser().parse(extraJson).getAsJsonObject();
        } else {
            extra = new JsonObject();
        }

        switch (type) {
            case 0:
            case 1:
            case 10:
            case 15:
            case 16:
                return new PlayerBlockLogEntry(resultSet, extra);
            case 17:
                return new PlayerTransactionEntry(resultSet, extra);
            case 18:
                return new PlayerUseEntry(resultSet, extra);
            case 19:
                return new PlayerSessionEntry(resultSet, extra);
            case 20:
                return new PlayerSignChangeEntry(resultSet, extra);
            case 3:
            case 4:
            case 5:
            case 7:
            case 21:
            case 22:
            case 33:
                return new PlayerItemLogEntry(resultSet, extra);
            case 8:
                return new CropGrowLogEntry(resultSet, extra);
            case 9:
                return new PlayerKillLogEntry(resultSet, extra);
            case 23:
                return new PlayerDeathEntry(resultSet, extra);
            case 11:
                return new PlayerTameEntry(resultSet, extra);
            case 12:
                return new PlayerBreedEntry(resultSet, extra);
            case 13:
                return new PlayerChatEntry(resultSet, extra);
            case 14:
                return new PlayerCommandEntry(resultSet, extra);
            case 24:
                return new PlayerMountEntry(resultSet, extra);
            case 26:
                return new PlayerHangingEntry(resultSet, extra);
            case 30:
                return new PlayerShootEntry(resultSet, extra);
            case 31:
                return new EntityResurrectEntry(resultSet, extra);
            case 32:
                return new PlayerTeleportLogEntry(resultSet, extra);
            case 34:
                return new PlayerGameModeLogEntry(resultSet, extra);
            case 35:
                return new PlayerXPEntry(resultSet, extra);
            case 36:
                return new PlayerEconomyEntry(resultSet, extra);
            default:
                return new LogEntry(resultSet);
        }
    }

}