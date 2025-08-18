package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.config.PatternConfig;
import io.github.insideranh.stellarprotect.config.WorldConfigType;
import io.github.insideranh.stellarprotect.config.suspicious.TpaSuspiciousConfig;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.SuspiciousType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final HashMap<SuspiciousType, PatternConfig> patternConfigs = new HashMap<>();
    @Setter
    private boolean debugLog;
    @Setter
    private boolean debugSave;
    @Setter
    private boolean debugExtras;
    private boolean liquidTracking;
    private int maxCores;
    private int batchSize;
    private int savePeriod;
    private int maxLogsPerLocation;
    private int deleteOldPeriod;
    private long timeForCacheClear;
    private long cleanPlacedCachePeriod;
    private int daysToKeepLogs;
    private int saveItemPeriod;
    private int saveBlockPeriod;
    private boolean suspiciousPatterns;
    private boolean checkUpdates;
    private boolean economyDisabled;
    private int economyCheckInterval;

    private String tablesPrefix;
    private String tablesPlayers;
    private String tablesLogEntries;
    private String tablesIdCounter;
    private String tablesItemTemplates;
    private String tablesBlockTemplates;
    private String tablesWorlds;
    private String tablesEntityIds;

    private String itemsLang;

    public void load() {
        this.checkUpdates = plugin.getConfig().getBoolean("check-updates");

        this.debugLog = plugin.getConfig().getBoolean("debugs.log");
        this.debugSave = plugin.getConfig().getBoolean("debugs.save");
        this.debugExtras = plugin.getConfig().getBoolean("debugs.extras");

        this.liquidTracking = plugin.getConfig().getBoolean("advanced.liquid-tracking");

        this.suspiciousPatterns = plugin.getConfig().getBoolean("features.suspicious-patterns.enabled");

        this.itemsLang = plugin.getConfig().getString("features.translations.items_lang");

        this.patternConfigs.put(SuspiciousType.TPA_KILL, new TpaSuspiciousConfig(plugin.getConfig(), "features.suspicious-patterns.suspicious-tpa-kill"));

        this.maxCores = Math.max(plugin.getConfig().getInt("optimizations.maxCores"), 4);
        this.savePeriod = plugin.getConfig().getInt("optimizations.save-period");
        this.batchSize = plugin.getConfig().getInt("optimizations.batch-size");
        this.maxLogsPerLocation = plugin.getConfig().getInt("optimizations.max-logs-per-location");
        this.timeForCacheClear = plugin.getConfig().getLong("optimizations.time-for-cache-clear") * 60 * 1000L;
        this.daysToKeepLogs = plugin.getConfig().getInt("optimizations.days-to-keep-logs");
        this.deleteOldPeriod = plugin.getConfig().getInt("optimizations.delete-old-period") * 60;
        this.cleanPlacedCachePeriod = plugin.getConfig().getLong("optimizations.clean-placed-cache-period") * 1000L;
        this.saveItemPeriod = plugin.getConfig().getInt("optimizations.item-save-period");
        this.saveBlockPeriod = plugin.getConfig().getInt("optimizations.block-save-period");
        this.economyDisabled = plugin.getConfig().getBoolean("optimizations.economy-disabled");
        this.economyCheckInterval = plugin.getConfig().getInt("optimizations.economy-check-interval");

        this.tablesPrefix = plugin.getConfig().getString("tablesOrCollections.prefix", "");
        this.tablesPlayers = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.players", "players");
        this.tablesLogEntries = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.log_entries", "log_entries");
        this.tablesIdCounter = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.id_counter", "id_counter");
        this.tablesItemTemplates = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.item_templates", "item_templates");
        this.tablesBlockTemplates = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.block_templates", "block_templates");
        this.tablesWorlds = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.worlds", "worlds");
        this.tablesEntityIds = this.tablesPrefix + plugin.getConfig().getString("tablesOrCollections.entity_ids", "entity_ids");

        HashMap<ActionType, HashMap<String, WorldConfigType>> worlds = new HashMap<>();

        File worldsFolder = new File(plugin.getDataFolder(), "worlds");
        if (!worldsFolder.exists()) {
            worldsFolder.mkdirs();
        }

        for (File file : Objects.requireNonNull(worldsFolder.listFiles())) {
            if (!file.isFile()) continue;
            String world = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (!config.isConfigurationSection("logs") || !config.getBoolean("enabled")) continue;

            for (ActionType actionType : ActionType.values()) {
                if (!config.isConfigurationSection("logs." + actionType.name().toLowerCase())) continue;

                WorldConfigType worldConfigType = new WorldConfigType();
                worldConfigType.setEnabled(config.getBoolean("logs." + actionType.name().toLowerCase() + ".enabled"));
                worldConfigType.getDisabledTypes().addAll(config.getStringList("logs." + actionType.name().toLowerCase() + ".disable_types").stream().map(String::toLowerCase).collect(Collectors.toList()));
                worlds.computeIfAbsent(actionType, k -> new HashMap<>()).put(world, worldConfigType);
            }
        }

        for (ActionType actionType : ActionType.values()) {
            HashMap<String, WorldConfigType> worldsConfig = worlds.getOrDefault(actionType, new HashMap<>());
            actionType.getWorldTypes().putAll(worldsConfig);

            boolean enabled = plugin.getConfig().getBoolean("logs." + actionType.name().toLowerCase() + ".enabled");
            List<String> worldList = plugin.getConfig().getStringList("logs." + actionType.name().toLowerCase() + ".worlds").stream().map(String::toLowerCase).collect(Collectors.toList());

            actionType.setEnabled(enabled);
            actionType.setHasAllWorlds(worldList.contains("all"));
            actionType.getWorlds().addAll(worldList);
            actionType.getDisabledTypes().addAll(plugin.getConfig().getStringList("logs." + actionType.name().toLowerCase() + ".disable_types").stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
    }

    public PatternConfig getPatternConfig(SuspiciousType suspiciousType) {
        return patternConfigs.get(suspiciousType);
    }

}