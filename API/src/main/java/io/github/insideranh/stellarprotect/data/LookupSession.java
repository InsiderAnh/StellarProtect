package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.PageArg;
import lombok.Getter;

@Getter
public class LookupSession {

    private final PageArg pageArg;
    private final DatabaseFilters databaseFilters;
    private final int skip;
    private final int limit;

    public LookupSession(PageArg pageArg, DatabaseFilters databaseFilters, int skip, int limit) {
        this.pageArg = pageArg;
        this.databaseFilters = databaseFilters;
        this.skip = skip;
        this.limit = limit;
    }

}