package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;

@Getter
@Setter
public class UndoSession {

    private final Player player;
    private final DatabaseFilters databaseFilters;
    private final HashMap<Integer, Object> processedLogHashes;
    private final HashMap<Integer, Object> undoneLogHashes;
    private final int logsPerPage;
    private int currentOffset;
    private boolean verbose;
    private boolean silent;

    public UndoSession(Player player, DatabaseFilters databaseFilters, boolean verbose, boolean silent) {
        this.player = player;
        this.databaseFilters = databaseFilters;
        this.processedLogHashes = new HashMap<>();
        this.undoneLogHashes = new HashMap<>();
        this.logsPerPage = 10;
        this.currentOffset = 0;
        this.verbose = verbose;
        this.silent = silent;
    }

    public void addProcessedLog(int logHash, Object logEntry) {
        processedLogHashes.put(logHash, logEntry);
    }

    public void markAsUndone(int logHash, Object logEntry) {
        undoneLogHashes.put(logHash, logEntry);
    }

    public void unmarkUndone(int logHash) {
        undoneLogHashes.remove(logHash);
    }

    public boolean isUndone(int logHash) {
        return undoneLogHashes.containsKey(logHash);
    }

    public boolean isProcessed(int logHash) {
        return processedLogHashes.containsKey(logHash);
    }

    public Object getProcessedLog(int logHash) {
        return processedLogHashes.get(logHash);
    }

    public Object getUndoneLog(int logHash) {
        return undoneLogHashes.get(logHash);
    }

    public void nextPage() {
        currentOffset += logsPerPage;
    }

}

