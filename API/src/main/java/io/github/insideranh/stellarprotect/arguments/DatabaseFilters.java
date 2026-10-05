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
    private List<String> includeEntityFilters = new ArrayList<>();
    private List<String> excludeEntityFilters = new ArrayList<>();
    private List<String> includeDisplayFilters = new ArrayList<>();
    private List<String> excludeDisplayFilters = new ArrayList<>();
    private List<String> includeLoreFilters = new ArrayList<>();
    private List<String> excludeLoreFilters = new ArrayList<>();
    private List<String> includeEnchantFilters = new ArrayList<>();
    private List<String> excludeEnchantFilters = new ArrayList<>();
    private Integer minAmount;
    private Integer maxAmount;
    private Integer chunkX;
    private Integer chunkZ;
    private String biomeFilter;
    private String toolFilter;
    private String sortBy = "time_desc";
    private String sortDirection = "desc";
    private boolean regex;
    private UsersArg userFilters;

    public boolean isIgnoreCache() {
        return !allIncludeFilters.isEmpty() || !allExcludeFilters.isEmpty()
                || !includeMaterialFilters.isEmpty() || !excludeMaterialFilters.isEmpty()
                || !includeBlockFilters.isEmpty() || !excludeBlockFilters.isEmpty()
                || !includeEntityFilters.isEmpty() || !excludeEntityFilters.isEmpty()
                || !includeDisplayFilters.isEmpty() || !excludeDisplayFilters.isEmpty()
                || !includeLoreFilters.isEmpty() || !excludeLoreFilters.isEmpty()
                || !includeEnchantFilters.isEmpty() || !excludeEnchantFilters.isEmpty()
                || minAmount != null || maxAmount != null
                || chunkX != null || chunkZ != null
                || (biomeFilter != null && !biomeFilter.isEmpty())
                || (toolFilter != null && !toolFilter.isEmpty())
                || regex;
    }

}