package io.github.insideranh.stellarprotect.database.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.*;
import io.github.insideranh.stellarprotect.database.types.mysql.*;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Getter
public class MySQLConnection implements DatabaseConnection {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private PlayerRepository playerRepository;
    private LoggerRepository loggerRepository;
    private IdsRepository idsRepository;
    private ItemsRepository itemsRepository;
    private BlocksRepository blocksRepository;
    private RestoreRepository restoreRepository;
    private HikariDataSource dataSource;

    @Override
    public void connect() {
        try {
            HikariConfig config = new HikariConfig();

            boolean useSSL = stellarProtect.getConfig().getBoolean("databases.mysql.useSSL", false);
            String url = "jdbc:mysql://" + stellarProtect.getConfig().getString("databases.mysql.host") + ":" +
                stellarProtect.getConfig().getInt("databases.mysql.port") + "/" +
                stellarProtect.getConfig().getString("databases.mysql.database") +
                "?autoReconnect=true";

            stellarProtect.getLogger().info("Connecting to MySQL database with url: " + url);

            config.setDriverClassName("com.mysql.jdbc.Driver");

            config.setJdbcUrl(url);
            config.setUsername(stellarProtect.getConfig().getString("databases.mysql.user"));
            config.setPassword(stellarProtect.getConfig().getString("databases.mysql.password", ""));

            config.addDataSourceProperty("cachePrepStmts", true);
            config.addDataSourceProperty("prepStmtCacheSize", 250);
            config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            config.addDataSourceProperty("useServerPrepStmts", true);
            config.addDataSourceProperty("useLocalSessionState", true);
            config.addDataSourceProperty("rewriteBatchedStatements", true);
            config.addDataSourceProperty("cacheResultSetMetadata", true);
            config.addDataSourceProperty("cacheServerConfiguration", true);
            config.addDataSourceProperty("elideSetAutoCommits", true);
            config.addDataSourceProperty("maintainTimeStats", false);
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.addDataSourceProperty("encoding", "UTF-8");
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("useSSL", useSSL);
            config.addDataSourceProperty("tcpKeepAlive", true);
            config.setMaxLifetime(Long.MAX_VALUE);
            config.setMinimumIdle(0);
            config.setIdleTimeout(30000L);
            config.setConnectionTimeout(10000L);
            config.setMaximumPoolSize(10);

            dataSource = new HikariDataSource(config);

            try (Connection connection = dataSource.getConnection()) {
                String playersTable = stellarProtect.getConfigManager().getTablesPlayers();
                String worldsTable = stellarProtect.getConfigManager().getTablesWorlds();
                String entityIdsTable = stellarProtect.getConfigManager().getTablesEntityIds();
                String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();
                String idCounterTable = stellarProtect.getConfigManager().getTablesIdCounter();
                String itemTemplatesTable = stellarProtect.getConfigManager().getTablesItemTemplates();
                String blockTemplatesTable = stellarProtect.getConfigManager().getTablesBlockTemplates();

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + playersTable + " (" +
                        "id BIGINT PRIMARY KEY," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "name VARCHAR(36)" +
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
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "player_id BIGINT," +
                        "world_id INT," +
                        "x DECIMAL(8, 2)," +
                        "y DECIMAL(8, 2)," +
                        "z DECIMAL(8, 2)," +
                        "action_type INT," +
                        "restored TINYINT DEFAULT 0," +
                        "extra_json TEXT," +
                        "created_at BIGINT" +
                        ")");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + idCounterTable + " (" +
                        "table_name VARCHAR(64) PRIMARY KEY," +
                        "current_id INTEGER)");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + itemTemplatesTable + " (" +
                        "id BIGINT PRIMARY KEY," +
                        "base64 TEXT," +
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

                    statement.execute("SET SESSION foreign_key_checks = 0");
                    statement.execute("SET SESSION unique_checks = 0");
                }
            }

            this.playerRepository = new PlayerRepositoryMySQL(dataSource);
            this.loggerRepository = new LoggerRepositoryMySQL(dataSource);
            this.idsRepository = new IdsRepositoryMySQL(dataSource);
            this.itemsRepository = new ItemsRepositoryMySQL(dataSource);
            this.blocksRepository = new BlocksRepositoryMySQL(dataSource);
            this.restoreRepository = new RestoreRepositoryMySQL(dataSource);

            stellarProtect.getLogger().info("Connected to MySQL database correctly.");
        } catch (Exception exception) {
            stellarProtect.getLogger().warning("Error on connect to MySQL database. " + exception.getMessage());
        }
    }

    @Override
    public void createIndexes() {
        String logEntries = stellarProtect.getConfigManager().getTablesLogEntries();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        String players = stellarProtect.getConfigManager().getTablesPlayers();

        try (Connection connection = dataSource.getConnection()) {
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
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().info("Failed to create indexes " + e.getMessage());
        }

        updateTables();
    }

    public void updateTables() {
        String logEntries = stellarProtect.getConfigManager().getTablesLogEntries();
        String players = stellarProtect.getConfigManager().getTablesPlayers();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE " + players + " ADD COLUMN realname VARCHAR(36);");
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE " + itemTemplates + " ADD COLUMN hash BIGINT;");
            }
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ALTER TABLE " + logEntries + " ADD COLUMN restored TINYINT DEFAULT 0;");
            }
        } catch (SQLException ex) {
            stellarProtect.getLogger().info("The realname and hash column already exists, ignoring...");
        }
    }


    @Override
    public void close() {
        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }

}