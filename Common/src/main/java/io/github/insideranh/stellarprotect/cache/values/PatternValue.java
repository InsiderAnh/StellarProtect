package io.github.insideranh.stellarprotect.cache.values;

import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import lombok.Data;

@Data
public class PatternValue {

    private long createdAt;
    private LogEntry logEntry;

    public PatternValue(LogEntry logEntry, long createdAt) {
        this.logEntry = logEntry;
        this.createdAt = createdAt;
    }

}