package io.github.insideranh.stellarprotect.database.entries.hooks;

import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import org.bson.Document;

import java.sql.ResultSet;

public class JobsIncomeEntry extends LogEntry {

    public JobsIncomeEntry(Document document, JsonObject jsonObject) {
        super(document);
    }

    public JobsIncomeEntry(ResultSet resultSet, JsonObject jsonObject) {
        super(resultSet);
    }

}
