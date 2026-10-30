package io.github.insideranh.stellarprotect.database.types;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.*;
import io.github.insideranh.stellarprotect.database.types.mysql.*;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.Getter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
                stellarProtect.getConfig().getString("databases.mysql.database");

            stellarProtect.getLogger().info("Connecting to MySQL database with url: " + url);

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
            config.setMaxLifetime(1800000L);
            config.setKeepaliveTime(300000L);
            config.setValidationTimeout(5000L);
            config.setMinimumIdle(1);
            config.setIdleTimeout(600000L);
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
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "player_id BIGINT," +
                        "world_id INT," +
                        "x DOUBLE," +
                        "y DOUBLE," +
                        "z DOUBLE," +
                        "action_type INT," +
                        "restored TINYINT DEFAULT 0," +
                        "extra_json TEXT," +
                        "created_at BIGINT," +
                        "block_id INT DEFAULT NULL," +
                        "old_block_id INT DEFAULT NULL," +
                        "item_id BIGINT DEFAULT NULL," +
                        "amount INT DEFAULT 0," +
                        "entity_type VARCHAR(48) DEFAULT NULL," +
                        "chunk_key BIGINT DEFAULT NULL" +
                        ")");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + idCounterTable + " (" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "count BIGINT NOT NULL" +
                        ")");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + itemTemplatesTable + " (" +
                        "id BIGINT PRIMARY KEY," +
                        "base64 TEXT," +
                        "hash BIGINT," +
                        "s TINYINT," +
                        "access_count INTEGER DEFAULT 0," +
                        "last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "total_used INTEGER DEFAULT 0" +
                        ")");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + blockTemplatesTable + " (" +
                        "id INT PRIMARY KEY," +
                        "block_data TEXT" +
                        ")");

                    String prefix = stellarProtect.getConfigManager().getTablesPrefix();
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "inv_snapshots (" +
                        "log_entry_id BIGINT NOT NULL," +
                        "slot SMALLINT NOT NULL," +
                        "item_id BIGINT DEFAULT NULL," +
                        "amount INT DEFAULT 0," +
                        "nbt TEXT," +
                        "PRIMARY KEY (log_entry_id, slot)," +
                        "INDEX idx_inv_item (item_id)" +
                        ")");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "inv_txns (" +
                        "log_entry_id BIGINT NOT NULL," +
                        "item_id BIGINT NOT NULL," +
                        "amount_delta INT NOT NULL," +
                        "is_added BOOLEAN NOT NULL," +
                        "PRIMARY KEY (log_entry_id, item_id, is_added)," +
                        "INDEX idx_txn_item (item_id)" +
                        ")");

                    statement.execute("SET SESSION foreign_key_checks = 0");
                    statement.execute("SET SESSION unique_checks = 0");
                }
                migrateTables(connection, playersTable, itemTemplatesTable, logEntriesTable);
            }

            this.playerRepository = new PlayerRepositoryMySQL(dataSource);
            this.loggerRepository = new LoggerRepositoryMySQL(dataSource);
            this.idsRepository = new IdsRepositoryMySQL(dataSource);
            this.itemsRepository = new ItemsRepositoryMySQL(dataSource);
            this.blocksRepository = new BlocksRepositoryMySQL(dataSource);
            this.restoreRepository = new RestoreRepositoryMySQL(dataSource);

            stellarProtect.getLogger().info("Connected to MySQL database correctly.");
        } catch (Exception exception) {
            stellarProtect.getLogger().severe("Error initializing MySQL database: " + exception.getMessage());
            if (dataSource != null) dataSource.close();
            throw new IllegalStateException("MySQL database initialization failed", exception);
        }
    }

    @Override
    public void createIndexes() {
        String logEntries = stellarProtect.getConfigManager().getTablesLogEntries();
        String itemTemplates = stellarProtect.getConfigManager().getTablesItemTemplates();
        String players = stellarProtect.getConfigManager().getTablesPlayers();

        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                executeIndexSql(stmt, "DROP INDEX idx_query_main ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_log_entries_optimized ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_action_time_coords ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_log_entries_filtering ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_query_optimized ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_covering_query ON " + logEntries + ";");
                executeIndexSql(stmt, "DROP INDEX idx_coords_time ON " + logEntries + ";");

                executeIndexSql(stmt, "DROP INDEX idx_item_hash ON " + itemTemplates + ";");
                executeIndexSql(stmt, "DROP INDEX idx_item_access_count ON " + itemTemplates + ";");
                executeIndexSql(stmt, "DROP INDEX idx_item_last_accessed ON " + itemTemplates + ";");
                executeIndexSql(stmt, "DROP INDEX idx_item_total_used ON " + itemTemplates + ";");

                executeIndexSql(stmt, "CREATE INDEX idx_block_time ON " + logEntries + " (block_id, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_oldblock_time ON " + logEntries + " (old_block_id, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_item_time ON " + logEntries + " (item_id, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_action_time ON " + logEntries + " (action_type, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_entity_time ON " + logEntries + " (entity_type, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_chunk ON " + logEntries + " (world_id, chunk_key, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_location_time_lookup ON " + logEntries + " (created_at DESC, x, y, z, action_type)");
                executeIndexSql(stmt, "CREATE INDEX idx_player_time ON " + logEntries + " (player_id, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_world_time ON " + logEntries + " (world_id, created_at)");
                executeIndexSql(stmt, "CREATE INDEX idx_time_action ON " + logEntries + " (created_at, action_type)");

                executeIndexSql(stmt, "CREATE INDEX idx_players_id ON " + players + " (id)");
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Failed to create indexes: " + e.getMessage());
        }
    }

    @Override
    public void vacuum() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("OPTIMIZE TABLE " + stellarProtect.getConfigManager().getTablesLogEntries() + ";");
            stmt.execute("OPTIMIZE TABLE " + stellarProtect.getConfigManager().getTablesItemTemplates() + ";");
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Failed to optimize tables: " + e.getMessage());
        }
    }

    public void updateTables() {
        try (Connection connection = dataSource.getConnection()) {
            migrateTables(connection, stellarProtect.getConfigManager().getTablesPlayers(),
                stellarProtect.getConfigManager().getTablesItemTemplates(),
                stellarProtect.getConfigManager().getTablesLogEntries());
        } catch (SQLException exception) {
            throw new IllegalStateException("MySQL schema migration failed", exception);
        }
    }

    private void migrateTables(Connection connection, String players, String itemTemplates, String logEntries) throws SQLException {
        ensureColumns(connection, players, new String[][]{
            {"realname", "VARCHAR(36)"}
        });
        ensureColumns(connection, itemTemplates, new String[][]{
            {"hash", "BIGINT"}
        });
        ensureColumns(connection, logEntries, new String[][]{
            {"restored", "TINYINT DEFAULT 0"},
            {"block_id", "INT DEFAULT NULL"},
            {"old_block_id", "INT DEFAULT NULL"},
            {"item_id", "BIGINT DEFAULT NULL"},
            {"amount", "INT DEFAULT 0"},
            {"entity_type", "VARCHAR(48) DEFAULT NULL"},
            {"chunk_key", "BIGINT DEFAULT NULL"}
        });
    }

    private void ensureColumns(Connection connection, String table, String[][] columns) throws SQLException {
        for (int attempt = 0; attempt < 3; attempt++) {
            Set<String> existing = new HashSet<>();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                for (int index = 1; index <= metadata.getColumnCount(); index++) {
                    existing.add(metadata.getColumnName(index).toLowerCase(Locale.ROOT));
                }
            }

            StringBuilder additions = new StringBuilder();
            for (String[] column : columns) {
                if (existing.contains(column[0].toLowerCase(Locale.ROOT))) continue;
                if (additions.length() > 0) additions.append(", ");
                additions.append("ADD COLUMN ").append(column[0]).append(' ').append(column[1]);
            }
            if (additions.length() == 0) return;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE " + table + " " + additions);
                stellarProtect.getLogger().info("MySQL table schema updated: " + table);
                return;
            } catch (SQLException exception) {
                if (exception.getErrorCode() != 1060 || attempt == 2) throw exception;
            }
        }
    }

    private static boolean executeIndexSql(Statement statement, String sql) throws SQLException {
        try {
            return statement.execute(sql);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1091 || e.getErrorCode() == 1061) return false;
            throw e;
        }
    }

    @Override
    public void close() {
        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }

}
