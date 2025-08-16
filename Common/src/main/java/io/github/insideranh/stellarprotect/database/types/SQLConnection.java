package io.github.insideranh.stellarprotect.database.types;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.*;
import io.github.insideranh.stellarprotect.database.types.sql.*;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Getter
public class SQLConnection implements DatabaseConnection {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private PlayerRepository playerRepository;
    private LoggerRepository loggerRepository;
    private IdsRepository idsRepository;
    private ItemsRepository itemsRepository;
    private RestoreRepository restoreRepository;
    private BlocksRepository blocksRepository;
    private Connection connection;

    @Override
    public void connect() {
        try {
            File dbFile = new File(stellarProtect.getDataFolder(), stellarProtect.getConfig().getString("databases.h2.database") + ".db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement statement = connection.createStatement()) {
                String playersTable = stellarProtect.getConfigManager().getTablesPlayers();
                String worldsTable = stellarProtect.getConfigManager().getTablesWorlds();
                String entityIdsTable = stellarProtect.getConfigManager().getTablesEntityIds();
                String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();
                String idCounterTable = stellarProtect.getConfigManager().getTablesIdCounter();
                String itemTemplatesTable = stellarProtect.getConfigManager().getTablesItemTemplates();
                String blockTemplatesTable = stellarProtect.getConfigManager().getTablesBlockTemplates();

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + playersTable + " (" +
                    "id BIGINT PRIMARY KEY," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "name VARCHAR(36)," +
                    "realname VARCHAR(36)" +
                    ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + worldsTable + " (" +
                    "id INT PRIMARY KEY," +
                    "name TEXT NOT NULL" +
                    ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + entityIdsTable + " (" +
                    "id BIGINT PRIMARY KEY," +
                    "entityType TEXT NOT NULL" +
                    ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + logEntriesTable + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_id BIGINT," +
                    "world_id INT," +
                    "x DECIMAL(8, 2)," +
                    "y DECIMAL(8, 2)," +
                    "z DECIMAL(8, 2)," +
                    "action_type INT," +
                    "extra_json TEXT," +
                    "created_at BIGINT," +
                    "FOREIGN KEY (player_id) REFERENCES " + playersTable + "(id)," +
                    "FOREIGN KEY (world_id) REFERENCES " + worldsTable + "(id)" +
                    ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + idCounterTable + " (" +
                    "table_name TEXT PRIMARY KEY," +
                    "current_id BIGINT)");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + itemTemplatesTable + " (" +
                    "id INTEGER PRIMARY KEY," +
                    "base64 TEXT," +
                    "hash INTEGER," +
                    "s TINYINT," +
                    "access_count INTEGER DEFAULT 0," +
                    "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "total_quantity_used INTEGER DEFAULT 0," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + blockTemplatesTable + " (" +
                    "id INT PRIMARY KEY," +
                    "block_data TEXT" +
                    ")");

            } catch (Exception exception) {
                stellarProtect.getLogger().warning("Error on connect to SQLite database " + exception.getMessage());
                exception.printStackTrace();
                return;
            }

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = 10000");
                stmt.execute("PRAGMA temp_store = MEMORY");
            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Failed to configure SQLite for performance");
            }

            this.playerRepository = new PlayerRepositorySQL(connection);
            this.loggerRepository = new LoggerRepositorySQL(connection);
            this.idsRepository = new IdsRepositorySQL(connection);
            this.itemsRepository = new ItemsRepositorySQL(connection);
            this.restoreRepository = new RestoreRepositorySQL(connection);
            this.blocksRepository = new BlocksRepositorySQL(connection);

            stellarProtect.getLogger().info("Connected to SQLite database correctly.");
        } catch (Exception exception) {
            stellarProtect.getLogger().warning("Error on connect to SQLite database " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @Override
    public void createIndexes() {
        String logEntries = stellarProtect.getConfigManager().getTablesLogEntries();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        String players = stellarProtect.getConfigManager().getTablesPlayers();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_query_main ON " + logEntries + " (created_at, action_type, x, y, z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_log_entries_optimized ON " + logEntries + " (created_at DESC, action_type, x, y, z, player_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_action_time_coords ON " + logEntries + " (action_type, created_at, x, y, z)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_log_entries_filtering ON " + logEntries + " (created_at, x, y, z)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_time_action ON " + logEntries + " (created_at, action_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_time ON " + logEntries + " (player_id, created_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_world_time ON " + logEntries + " (world_id, created_at)");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_query_optimized ON " + logEntries + " (x, y, z, created_at DESC, action_type, player_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_covering_query ON " + logEntries + " (x, y, z, created_at, action_type, player_id, id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_coords_time ON " + logEntries + " (x, y, z, created_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_id ON " + players + " (id);");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_hash ON " + itemTemplates + " (base64)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_access_count ON " + itemTemplates + " (access_count DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_last_accessed ON " + itemTemplates + " (last_accessed DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_item_total_used ON " + itemTemplates + " (total_quantity_used DESC)");
        } catch (SQLException e) {
            stellarProtect.getLogger().info("Failed to create indexes");
        }

        updateTables();
    }

    public void updateTables() {
        String players = stellarProtect.getConfigManager().getTablesPlayers();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + players + " ADD COLUMN realname VARCHAR(36);");
        } catch (SQLException ex) {
            stellarProtect.getLogger().info("The realname column already exists, ignoring...");
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + itemTemplates + " ADD COLUMN hash INTEGER;");
        } catch (SQLException ex) {
            stellarProtect.getLogger().info("The hash column already exists, ignoring...");
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error on close SQLite connection: " + e.getMessage());
        }
    }

}
