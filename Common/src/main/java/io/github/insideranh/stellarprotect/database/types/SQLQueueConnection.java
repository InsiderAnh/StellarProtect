package io.github.insideranh.stellarprotect.database.types;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.QueuedLog;
import io.github.insideranh.stellarprotect.utils.Debugger;
import lombok.Getter;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class SQLQueueConnection {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final AtomicLong lastProcessedId = new AtomicLong(0);
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Connection connection;
    private PreparedStatement insertStatement;

    public void connect() {
        try {
            File dbFile = new File(stellarProtect.getDataFolder(), "temp_queue.db");
            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA synchronous = OFF");
                stmt.execute("PRAGMA journal_mode = MEMORY");
                stmt.execute("PRAGMA temp_store = MEMORY");
                stmt.execute("PRAGMA locking_mode = EXCLUSIVE");
                stmt.execute("PRAGMA cache_size = -32000");
                stmt.execute("PRAGMA count_changes = OFF");
                stmt.execute("PRAGMA auto_vacuum = NONE");
                stmt.execute("PRAGMA page_size = 8192");
            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Failed to configure SQLite for maximum performance");
                e.printStackTrace();
            }

            try (Statement statement = connection.createStatement()) {
                String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + logEntriesTable + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_id INTEGER," +
                    "world_id INTEGER," +
                    "x REAL," +
                    "y REAL," +
                    "z REAL," +
                    "action_type INTEGER," +
                    "restored INTEGER DEFAULT 0," +
                    "extra_json TEXT," +
                    "created_at INTEGER" +
                    ")");

            } catch (Exception exception) {
                stellarProtect.getLogger().warning("Error creating queue table: " + exception.getMessage());
                exception.printStackTrace();
                return;
            }

            connection.setAutoCommit(false);

            String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();
            insertStatement = connection.prepareStatement(
                "INSERT INTO " + logEntriesTable +
                    " (player_id, world_id, x, y, z, action_type, restored, extra_json, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );

            stellarProtect.getLogger().info("Queue database connected.");

        } catch (Exception exception) {
            stellarProtect.getLogger().warning("Error on connect to SQLite queue database: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void save(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return;
        }

        singleThreadExecutor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                for (LogEntry logEntry : logEntries) {
                    String extraJson = logEntry.toSaveJson();

                    insertStatement.setLong(1, logEntry.getPlayerId());
                    insertStatement.setInt(2, logEntry.getWorldId());
                    insertStatement.setDouble(3, logEntry.getX());
                    insertStatement.setDouble(4, logEntry.getY());
                    insertStatement.setDouble(5, logEntry.getZ());
                    insertStatement.setInt(6, logEntry.getActionType());
                    insertStatement.setInt(7, 0);
                    insertStatement.setString(8, extraJson);
                    insertStatement.setLong(9, System.currentTimeMillis());
                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();
                connection.commit();

                Debugger.debugSave("Flushed " + logEntries.size() + " logs to queue in " + (System.currentTimeMillis() - startTime) + " ms");
            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Error flushing queue: " + e.getMessage());
                e.printStackTrace();
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    stellarProtect.getLogger().warning("Error rolling back: " + ex.getMessage());
                }
            }
        });
    }

    public List<QueuedLog> getLogs(int maxLogs) {
        List<QueuedLog> logs = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();

            long currentLastId = lastProcessedId.get();

            stmt = connection.prepareStatement(
                "SELECT * FROM " + logEntriesTable +
                    " WHERE id > ? ORDER BY id LIMIT ?"
            );
            stmt.setLong(1, currentLastId);
            stmt.setInt(2, maxLogs);

            rs = stmt.executeQuery();

            long maxId = currentLastId;
            while (rs.next()) {
                long id = rs.getLong("id");
                maxId = Math.max(maxId, id);

                QueuedLog log = QueuedLog.builder()
                    .id(id)
                    .playerId(rs.getLong("player_id"))
                    .worldId(rs.getInt("world_id"))
                    .x(rs.getDouble("x"))
                    .y(rs.getDouble("y"))
                    .z(rs.getDouble("z"))
                    .actionType(rs.getInt("action_type"))
                    .restored(rs.getInt("restored") == 1)
                    .extraJson(rs.getString("extra_json"))
                    .createdAt(rs.getLong("created_at"))
                    .build();

                logs.add(log);
            }

            if (!logs.isEmpty()) {
                lastProcessedId.set(maxId);
                Debugger.debugSave("Fetched " + logs.size() + " logs from queue (IDs " + logs.get(0).getId() + " to " + maxId + ")");
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error getting logs from queue: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Error closing resources: " + e.getMessage());
            }
        }

        return logs;
    }

    public void deleteProcessedLogs(long maxId) {
        singleThreadExecutor.execute(() -> {
            PreparedStatement stmt = null;
            PreparedStatement countStmt = null;
            ResultSet countRs = null;

            try {
                String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();

                stmt = connection.prepareStatement(
                    "DELETE FROM " + logEntriesTable + " WHERE id <= ?"
                );
                stmt.setLong(1, maxId);

                int deleted = stmt.executeUpdate();
                connection.commit();

                if (deleted > 0) {
                    Debugger.debugSave("Deleted " + deleted + " processed logs from queue (up to ID " + maxId + ")");
                }

                countStmt = connection.prepareStatement(
                    "SELECT COUNT(*) as remaining FROM " + logEntriesTable
                );
                countRs = countStmt.executeQuery();
                int remaining = 0;
                if (countRs.next()) {
                    remaining = countRs.getInt("remaining");
                }

                Debugger.debugSave(remaining + " logs remaining in queue");
            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Error deleting processed logs: " + e.getMessage());
                e.printStackTrace();
                try {
                    if (connection != null) {
                        connection.rollback();
                    }
                } catch (SQLException ex) {
                    stellarProtect.getLogger().warning("Error rolling back delete: " + ex.getMessage());
                }
            } finally {
                try {
                    if (countRs != null) countRs.close();
                    if (countStmt != null) countStmt.close();
                    if (stmt != null) stmt.close();
                } catch (SQLException e) {
                    stellarProtect.getLogger().warning("Error closing resources: " + e.getMessage());
                }
            }
        });
    }

    public void close() {
        try {
            singleThreadExecutor.shutdown();
            if (!singleThreadExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                stellarProtect.getLogger().warning("Executor did not terminate in time, forcing shutdown...");
                singleThreadExecutor.shutdownNow();
            }

            if (insertStatement != null && !insertStatement.isClosed()) {
                insertStatement.close();
            }

            if (connection != null && !connection.isClosed()) {
                connection.commit();
                connection.close();
            }

            stellarProtect.getLogger().info("Queue database closed.");
        } catch (Exception e) {
            stellarProtect.getLogger().warning("Error on close SQLite connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
