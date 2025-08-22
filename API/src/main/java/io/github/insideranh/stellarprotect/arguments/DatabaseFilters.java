package io.github.insideranh.stellarprotect.arguments;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatabaseFilters {

    private LocationArg locationFilter;
    private TimeArg timeFilter;
    private RadiusArg radiusFilter;
    private PageArg pageFilter;
    private List<Integer> actionTypesFilter = new ArrayList<>();
    private List<Integer> actionTypesExcludeFilter = new ArrayList<>();
    private List<Long> wordsFilter = new ArrayList<>();
    private List<Long> wordsExcludeFilter = new ArrayList<>();
    private UsersArg userFilters;

    public boolean isIgnoreCache() {
        return !wordsFilter.isEmpty() || !wordsExcludeFilter.isEmpty();
    }

}
