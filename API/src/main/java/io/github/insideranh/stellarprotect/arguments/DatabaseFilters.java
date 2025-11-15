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
    private List<Long> allIncludeFilters = new ArrayList<>();
    private List<Long> allExcludeFilters = new ArrayList<>();
    private List<Long> includeMaterialFilters = new ArrayList<>();
    private List<Long> excludeMaterialFilters = new ArrayList<>();
    private List<Long> includeBlockFilters = new ArrayList<>();
    private List<Long> excludeBlockFilters = new ArrayList<>();
    private UsersArg userFilters;

    public boolean isIgnoreCache() {
        return !allIncludeFilters.isEmpty() || !allExcludeFilters.isEmpty() || !includeMaterialFilters.isEmpty() || !excludeMaterialFilters.isEmpty()
            || !includeBlockFilters.isEmpty() || !excludeBlockFilters.isEmpty();
    }

}
