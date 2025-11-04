package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;

@Getter
@Setter
public class RestoreSession {

    private final Player player;
    private final DatabaseFilters databaseFilters;
    private final HashMap<Integer, Object> processedLogHashes;
    private final HashMap<Integer, Object> restoredLogHashes;
    private final int logsPerPage;
    private int currentOffset;
    private boolean verbose;
    private boolean silent;

    public RestoreSession(Player player, DatabaseFilters databaseFilters, boolean verbose, boolean silent) {
        this.player = player;
        this.databaseFilters = databaseFilters;
        this.processedLogHashes = new HashMap<>();
        this.restoredLogHashes = new HashMap<>();
        this.logsPerPage = 10;
        this.currentOffset = 0;
        this.verbose = verbose;
        this.silent = silent;
    }

    public void addProcessedLog(int logHash, Object logEntry) {
        processedLogHashes.put(logHash, logEntry);
    }

    public void markAsRestored(int logHash, Object logEntry) {
        restoredLogHashes.put(logHash, logEntry);
    }

    public void unmarkRestored(int logHash) {
        restoredLogHashes.remove(logHash);
    }

    public boolean isRestored(int logHash) {
        return restoredLogHashes.containsKey(logHash);
    }

    public boolean isProcessed(int logHash) {
        return processedLogHashes.containsKey(logHash);
    }

    public Object getProcessedLog(int logHash) {
        return processedLogHashes.get(logHash);
    }

    public Object getRestoredLog(int logHash) {
        return restoredLogHashes.get(logHash);
    }

    public void nextPage() {
        currentOffset += logsPerPage;
    }

}