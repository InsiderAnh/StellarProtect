package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.repositories.PlayerRepository;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerRepositoryMongo implements PlayerRepository {

    private final MongoDatabase database;
    private final MongoCollection<Document> players;

    public PlayerRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.players = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesPlayers());
    }

    @Override
    public PlayerProtect loadOrCreatePlayer(Player player) {
        Document doc = players.find(Filters.eq("uuid", player.getUniqueId().toString())).first();
        if (doc != null) {
            return new PlayerProtect(player.getUniqueId(), player.getName(), doc.getLong("id"));
        }

        PlayerProtect playerProtect = new PlayerProtect(player.getUniqueId(), player.getName(), generateNextId());

        Document playerDoc = new Document("uuid", player.getUniqueId().toString())
            .append("name", player.getName().toLowerCase())
            .append("id", playerProtect.getPlayerId());

        players.insertOne(playerDoc);
        return playerProtect;
    }

    @Override
    public List<Long> getIdsByNames(List<String> names) {
        List<Long> ids = new ArrayList<>();
        if (names.isEmpty()) return ids;

        HashMap<String, Long> foundPlayers = new HashMap<>();
        try (MongoCursor<Document> cursor = players.find(Filters.in("name", names)).cursor()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Object idObj = doc.get("id");
                if (idObj instanceof Number) {
                    ids.add(((Number) idObj).longValue());
                    foundPlayers.put(doc.getString("name").toLowerCase(), ((Number) idObj).longValue());
                }
            }
        }
        for (String name : names) {
            if (!foundPlayers.containsKey(name.toLowerCase())) {
                ids.add(-2L);
            }
        }
        return ids;
    }

    @Override
    public long generateNextId() {
        Document result = database.getCollection("counters").findOneAndUpdate(
            Filters.eq("_id", "players"),
            Updates.inc("seq", 1),
            new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        );

        if (result != null && result.containsKey("seq")) {
            return result.getLong("seq");
        } else {
            throw new NullPointerException("No se pudo generar un nuevo ID para players.");
        }
    }

}