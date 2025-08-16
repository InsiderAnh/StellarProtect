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
    private MongoCollection<Document> players;
    private MongoCollection<Document> itemTemplates;
    private PlayerRepository playerRepository;
    private LoggerRepository loggerRepository;
    private IdsRepository idsRepository;
    private ItemsRepository itemsRepository;
    private BlocksRepository blocksRepository;
    private RestoreRepository restoreRepository;

    @Override
    public void connect() {
        String connectionString = "mongodb://" + stellarProtect.getConfig().getString("databases.mongodb.user") + ":" + stellarProtect.getConfig().getString("databases.mongodb.password") + "@" + stellarProtect.getConfig().getString("databases.mongodb.host") + ":" + stellarProtect.getConfig().getInt("databases.mongodb.port");

        try {
            this.mongoClient = MongoClients.create(new ConnectionString(connectionString));
            MongoDatabase database = mongoClient.getDatabase(stellarProtect.getConfig().getString("databases.mongodb.database", "StellarProtect"));
            this.logEntries = database.getCollection(stellarProtect.getConfigManager().getTablesLogEntries());
            this.players = database.getCollection(stellarProtect.getConfigManager().getTablesPlayers());
            this.itemTemplates = database.getCollection(stellarProtect.getConfigManager().getTablesItemTemplates());

            this.playerRepository = new PlayerRepositoryMongo(database);
            this.loggerRepository = new LoggerRepositoryMongo(database);
            this.idsRepository = new IdsRepositoryMongo(database);
            this.itemsRepository = new ItemsRepositoryMongo(database);
            this.blocksRepository = new BlocksRepositoryMongo(database);
            this.restoreRepository = new RestoreRepositoryMongo(database);

            stellarProtect.getLogger().info("Connected to Mongo database correctly.");
        } catch (Exception exception) {
            stellarProtect.getLogger().warning("Error on connect to Mongo database.");
        }
    }

    @Override
    public void createIndexes() {
        try {
            // === ÍNDICES PARA LOG_ENTRIES ===

            // idx_query_main: (created_at, action_type, x, y, z)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("created_at"),
                    Indexes.ascending("action_type"),
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z")
                ),
                new IndexOptions().background(true).name("idx_query_main")
            );

            // idx_log_entries_optimized: (created_at DESC, action_type, x, y, z, player_id)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.descending("created_at"),
                    Indexes.ascending("action_type"),
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z"),
                    Indexes.ascending("player_id")
                ),
                new IndexOptions().background(true).name("idx_log_entries_optimized")
            );

            // idx_action_time_coords: (action_type, created_at, x, y, z)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("action_type"),
                    Indexes.ascending("created_at"),
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z")
                ),
                new IndexOptions().background(true).name("idx_action_time_coords")
            );

            // idx_log_entries_filtering: (created_at, x, y, z)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("created_at"),
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z")
                ),
                new IndexOptions().background(true).name("idx_log_entries_filtering")
            );

            // idx_time_action: (created_at, action_type)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("created_at"),
                    Indexes.ascending("action_type")
                ),
                new IndexOptions().background(true).name("idx_time_action")
            );

            // idx_player_time: (player_id, created_at)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("player_id"),
                    Indexes.ascending("created_at")
                ),
                new IndexOptions().background(true).name("idx_player_time")
            );

            // idx_world_time: (world_id, created_at)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("world_id"),
                    Indexes.ascending("created_at")
                ),
                new IndexOptions().background(true).name("idx_world_time")
            );

            // idx_query_optimized: (x, y, z, created_at DESC, action_type, player_id)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z"),
                    Indexes.descending("created_at"),
                    Indexes.ascending("action_type"),
                    Indexes.ascending("player_id")
                ),
                new IndexOptions().background(true).name("idx_query_optimized")
            );

            // idx_covering_query: (x, y, z, created_at, action_type, player_id, id)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z"),
                    Indexes.ascending("created_at"),
                    Indexes.ascending("action_type"),
                    Indexes.ascending("player_id"),
                    Indexes.ascending("id")
                ),
                new IndexOptions().background(true).name("idx_covering_query")
            );

            // idx_coords_time: (x, y, z, created_at DESC)
            logEntries.createIndex(
                Indexes.compoundIndex(
                    Indexes.ascending("x"),
                    Indexes.ascending("y"),
                    Indexes.ascending("z"),
                    Indexes.descending("created_at")
                ),
                new IndexOptions().background(true).name("idx_coords_time")
            );

            // idx_players_id: (id)
            players.createIndex(
                Indexes.ascending("id"),
                new IndexOptions().background(true).name("idx_players_id")
            );

            // idx_item_hash: (base64)
            itemTemplates.createIndex(
                Indexes.ascending("base64"),
                new IndexOptions().background(true).name("idx_item_hash")
            );

            // idx_item_access_count: (access_count DESC)
            itemTemplates.createIndex(
                Indexes.descending("access_count"),
                new IndexOptions().background(true).name("idx_item_access_count")
            );

            // idx_item_last_accessed: (last_accessed DESC)
            itemTemplates.createIndex(
                Indexes.descending("last_accessed"),
                new IndexOptions().background(true).name("idx_item_last_accessed")
            );

            // idx_item_total_used: (total_quantity_used DESC)
            itemTemplates.createIndex(
                Indexes.descending("total_quantity_used"),
                new IndexOptions().background(true).name("idx_item_total_used")
            );

            stellarProtect.getLogger().info("All MongoDB indexes created successfully");
        } catch (Exception e) {
            stellarProtect.getLogger().info("Failed to create MongoDB indexes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
    }

}