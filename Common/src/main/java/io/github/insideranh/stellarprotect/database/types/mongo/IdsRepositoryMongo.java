package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.IdsRepository;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bson.Document;

public class IdsRepositoryMongo implements IdsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final MongoDatabase database;
    private final MongoCollection<Document> worlds;
    private final MongoCollection<Document> entityIds;

    public IdsRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.worlds = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesWorlds());
        this.entityIds = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesEntityIds());
    }

    @Override
    public void loadWorlds() {
        FindIterable<Document> findWorlds = worlds.find();
        for (Document doc : findWorlds) {
            try {
                String world = doc.getString("name");
                int id = doc.getInteger("id");

                WorldUtils.cacheWorld(world, id);
                Debugger.debugExtras("Loaded world " + world + " with id " + id);
            } catch (Exception ignored) {
                String data = doc.getString("data");
                Debugger.debugLog("Failed to load world: " + data);
            }
        }
    }

    @Override
    public void loadEntityIds() {
        FindIterable<Document> findEntityIds = entityIds.find();
        for (Document doc : findEntityIds) {
            try {
                String entityType = doc.getString("entityType");
                int id = doc.getInteger("id");

                PlayerUtils.cacheEntityId(entityType, id);
                Debugger.debugExtras("Loaded entity " + entityType + " with id " + id);
            } catch (Exception ignored) {
                String data = doc.getString("data");
                Debugger.debugLog("Failed to load entity: " + data);
            }

            PlayerUtils.loadEntityIds();
        }
    }

    @Override
    public void saveWorld(String world, int id) {
        try {
            Document doc = new Document("name", world).append("id", id);
            worlds.insertOne(doc);

            Debugger.debugExtras("Saved world " + world + " with id " + id);
        } catch (Exception e) {
            stellarProtect.getLogger().warning("Error on saveWorld: " + e.getMessage());
        }
    }

    @Override
    public void saveEntityId(String entityType, long id) {
        try {
            Document doc = new Document("entityType", entityType).append("id", id);
            entityIds.insertOne(doc);

            Debugger.debugExtras("Saved entityType " + entityType + " with id " + id);
        } catch (Exception e) {
            stellarProtect.getLogger().warning("Error on saveEntityId: " + e.getMessage());
        }
    }

}