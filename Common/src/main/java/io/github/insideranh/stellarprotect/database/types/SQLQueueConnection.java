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
                stmt.execute("PRAGMA cache_size = -128000");
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
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    stellarProtect.getLogger().warning("Error rolling back: " + ex.getMessage());
                }
            }
        });
    }

    public List<QueuedLog> getLogs(int maxLogs) {
        try {
            return singleThreadExecutor.submit(() -> {
                List<QueuedLog> logs = new ArrayList<>();
                try {
                    String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();

                    PreparedStatement countStmt = connection.prepareStatement(
                        "SELECT COUNT(*) as total FROM " + logEntriesTable
                    );
                    ResultSet countRs = countStmt.executeQuery();
                    int totalLogs = 0;
                    if (countRs.next()) {
                        totalLogs = countRs.getInt("total");
                    }
                    countRs.close();
                    countStmt.close();

                    if (totalLogs == 0) {
                        lastProcessedId.set(0);
                        Debugger.debugSave("Queue is empty, reset lastProcessedId to 0");
                        return logs;
                    }

                    long currentLastId = lastProcessedId.get();
                    if (currentLastId == 0) {
                        PreparedStatement minStmt = connection.prepareStatement(
                            "SELECT MIN(id) as min_id FROM " + logEntriesTable
                        );
                        ResultSet minRs = minStmt.executeQuery();
                        if (minRs.next()) {
                            long minId = minRs.getLong("min_id");
                            if (minId > 0) {
                                currentLastId = minId - 1;
                                lastProcessedId.set(currentLastId);
                                Debugger.debugSave("Initialized lastProcessedId to " + currentLastId + " (min_id - 1)");
                            }
                        }
                        minRs.close();
                        minStmt.close();
                    }

                    PreparedStatement stmt = connection.prepareStatement(
                        "SELECT * FROM " + logEntriesTable +
                            " WHERE id > ? ORDER BY id LIMIT ?"
                    );
                    stmt.setLong(1, currentLastId);
                    stmt.setInt(2, maxLogs);

                    ResultSet rs = stmt.executeQuery();

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
                        Debugger.debugSave("Fetched " + logs.size() + " logs from queue, updated lastProcessedId to " + maxId);
                    }

                    rs.close();
                    stmt.close();

                } catch (SQLException e) {
                    stellarProtect.getLogger().warning("Error getting logs from queue: " + e.getMessage());
                    e.printStackTrace();
                }

                return logs;
            }).get();
        } catch (Exception e) {
            stellarProtect.getLogger().warning("Error executing getLogs: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void deleteProcessedLogs(long maxId) {
        singleThreadExecutor.execute(() -> {
            try {
                String logEntriesTable = stellarProtect.getConfigManager().getTablesLogEntries();

                PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM " + logEntriesTable + " WHERE id <= ?"
                );
                stmt.setLong(1, maxId);

                int deleted = stmt.executeUpdate();
                connection.commit();

                if (deleted > 0) {
                    Debugger.debugSave("Deleted " + deleted + " processed logs from queue");
                }

                stmt.close();

                PreparedStatement countStmt = connection.prepareStatement(
                    "SELECT COUNT(*) as remaining FROM " + logEntriesTable
                );
                ResultSet countRs = countStmt.executeQuery();
                int remaining = 0;
                if (countRs.next()) {
                    remaining = countRs.getInt("remaining");
                }
                countRs.close();
                countStmt.close();

                if (remaining == 0) {
                    lastProcessedId.set(0);
                    Debugger.debugSave("Queue is now empty, reset lastProcessedId to 0");
                } else {
                    Debugger.debugSave(remaining + " logs remaining in queue");
                }

            } catch (SQLException e) {
                stellarProtect.getLogger().warning("Error deleting processed logs: " + e.getMessage());
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    stellarProtect.getLogger().warning("Error rolling back delete: " + ex.getMessage());
                }
            }
        });
    }

    public void close() {
        try {
            singleThreadExecutor.shutdown();
            if (!singleThreadExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                singleThreadExecutor.shutdownNow();
            }

            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            stellarProtect.getLogger().warning("Error on close SQLite connection: " + e.getMessage());
        }
    }

}
