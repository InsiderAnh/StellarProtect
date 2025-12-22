package io.github.insideranh.stellarprotect.database.types;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.*;
import io.github.insideranh.stellarprotect.database.types.sql.*;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import lombok.Getter;

import java.io.File;
import java.sql.*;

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
                    "restored TINYINT DEFAULT 0," +
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

                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + logEntriesTable);
                if (resultSet.next()) {
                    long count = resultSet.getLong("COUNT(*)");
                    PlayerUtils.setNextLogId(count);
                    stellarProtect.getLogger().info("Next log ID: " + count);
                }
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
        String players = stellarProtect.getConfigManager().getTablesPlayers();

        try (Statement stmt = connection.createStatement()) {
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_query_main;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_log_entries_optimized;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_action_time_coords;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_log_entries_filtering;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_query_optimized;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_covering_query;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_coords_time;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_item_hash;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_item_access_count;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_item_last_accessed;");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("DROP INDEX IF EXISTS idx_item_total_used;");
            } catch (SQLException ignored) {
            }

            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_time_lookup ON " + logEntries + " (created_at DESC, x, y, z, action_type)");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_time ON " + logEntries + " (player_id, created_at)");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_world_time ON " + logEntries + " (world_id, created_at)");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_time_action ON " + logEntries + " (created_at, action_type)");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_id ON " + players + " (id)");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Failed to create indexes: " + e.getMessage());
        }

        updateTables();
    }

    @Override
    public void vacuum() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("VACUUM;");
            stellarProtect.getLogger().info("SQLite database vacuumed successfully.");
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Failed to vacuum SQLite database: " + e.getMessage());
        }
    }

    public void updateTables() {
        String logEntries = stellarProtect.getConfigManager().getTablesLogEntries();
        String players = stellarProtect.getConfigManager().getTablesPlayers();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + players + " ADD COLUMN realname VARCHAR(36);");
        } catch (SQLException ignored) {
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + itemTemplates + " ADD COLUMN hash INTEGER;");
        } catch (SQLException ignored) {
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + logEntries + " ADD COLUMN restored TINYINT DEFAULT 0;");
        } catch (SQLException ignored) {
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
