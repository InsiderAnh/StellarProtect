package io.github.insideranh.stellarprotect.database.types;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.*;
import io.github.insideranh.stellarprotect.database.types.mongo.*;
import lombok.Getter;
import org.bson.Document;

@Getter
public class MongoConnection implements DatabaseConnection {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private MongoClient mongoClient;
    private MongoCollection<Document> logEntries;
    private PlayerRepository playerRepository;
    private LoggerRepository loggerRepository;
    private IdsRepository idsRepository;
    private ItemsRepository itemsRepository;
    private RestoreRepository restoreRepository;

    @Override
    public void connect() {
        String connectionString = "mongodb://" + stellarProtect.getConfig().getString("databases.mongodb.user") + ":" + stellarProtect.getConfig().getString("databases.mongodb.password") + "@" + stellarProtect.getConfig().getString("databases.mongodb.host") + ":" + stellarProtect.getConfig().getInt("databases.mongodb.port");

        try {
            this.mongoClient = MongoClients.create(new ConnectionString(connectionString));
            MongoDatabase database = mongoClient.getDatabase(stellarProtect.getConfig().getString("databases.mongodb.database", "StellarProtect"));
            this.logEntries = database.getCollection(stellarProtect.getConfigManager().getTablesLogEntries());

            this.playerRepository = new PlayerRepositoryMongo(database);
            this.loggerRepository = new LoggerRepositoryMongo(database);
            this.idsRepository = new IdsRepositoryMongo(database);
            this.itemsRepository = new ItemsRepositoryMongo(database);
            this.restoreRepository = new RestoreRepositoryMongo(database);

            stellarProtect.getLogger().info("Connected to Mongo database correctly.");
        } catch (Exception exception) {
            stellarProtect.getLogger().warning("Error on connect to Mongo database.");
        }
    }

    @Override
    public void createIndexes() {
        MongoCollection<Document> collection = this.logEntries;

        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.descending("created_at"),
                Indexes.ascending("action_type"),
                Indexes.ascending("x"),
                Indexes.ascending("y"),
                Indexes.ascending("z"),
                Indexes.ascending("player_id")
            )
        );

        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("created_at"),
                Indexes.ascending("x"),
                Indexes.ascending("y"),
                Indexes.ascending("z"),
                Indexes.ascending("action_type")
            ),
            new IndexOptions().background(true)
        );

        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("action_type"),
                Indexes.ascending("created_at"),
                Indexes.ascending("x"),
                Indexes.ascending("y"),
                Indexes.ascending("z")
            ),
            new IndexOptions().background(true)
        );

        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("player_id"),
                Indexes.ascending("created_at")
            ),
            new IndexOptions().background(true)
        );

        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("world_id"),
                Indexes.ascending("created_at")
            ),
            new IndexOptions().background(true)
        );
    }

    @Override
    public void close() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }

}