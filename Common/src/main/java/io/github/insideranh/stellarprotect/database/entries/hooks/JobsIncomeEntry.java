package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;

import java.sql.ResultSet;

public class JobsIncomeEntry extends LogEntry {

    public JobsIncomeEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
    }

}
