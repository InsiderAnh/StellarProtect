package io.github.insideranh.stellarprotect.cache;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.values.PatternValue;
import io.github.insideranh.stellarprotect.config.suspicious.TpaSuspiciousConfig;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerDeathEntry;
import io.github.insideranh.stellarprotect.database.entries.players.chat.PlayerCommandEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.SuspiciousType;
import io.github.insideranh.stellarprotect.managers.ConfigManager;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PlayerCache {

    private static final ConfigManager configManager = StellarProtect.getInstance().getConfigManager();
    private static final Map<Long, String> playerNames = new HashMap<>();
    private static final Map<Long, EnumMap<ActionType, PatternValue>> patterns = new HashMap<>();

    public static void checkPattern(LogEntry logEntry) {
        if (!configManager.isSuspiciousPatterns()) return;

        TpaSuspiciousConfig config = getTPAConfig();
        if (config.isEnabled()) {
            if (logEntry instanceof PlayerCommandEntry) {
                handleCommand((PlayerCommandEntry) logEntry, config);
            } else if (logEntry instanceof PlayerDeathEntry) {
                handleKill((PlayerDeathEntry) logEntry);
            }
        }
    }

    private static void handleCommand(PlayerCommandEntry entry, TpaSuspiciousConfig config) {
        String command = entry.getCommand().split(" ")[0];
        if (!config.getCheckCommands().contains(command)) return;

        patterns
            .computeIfAbsent(entry.getPlayerId(), k -> new EnumMap<>(ActionType.class))
            .put(ActionType.COMMAND, new PatternValue(entry, System.currentTimeMillis()));
    }

    private static void handleKill(PlayerDeathEntry entry) {
        if (entry.getActionType() != ActionType.DEATH.getId()) return;

        patterns
            .computeIfAbsent(entry.getPlayerId(), k -> new EnumMap<>(ActionType.class))
            .put(ActionType.DEATH, new PatternValue(entry, System.currentTimeMillis()));

        checkTPAKill(entry.getPlayerId());
    }

    private static void checkTPAKill(long playerId) {
        Map<ActionType, PatternValue> playerPatterns = patterns.get(playerId);
        if (playerPatterns == null) return;

        PatternValue commandPattern = playerPatterns.get(ActionType.COMMAND);
        PatternValue deathPattern = playerPatterns.get(ActionType.DEATH);
        if (commandPattern == null || deathPattern == null) return;

        TpaSuspiciousConfig config = getTPAConfig();
        long currentTime = System.currentTimeMillis();

        if (commandPattern.getCreatedAt() + config.getExpireCommandTpa() <= currentTime) return;

        notifyPlayers(playerId, (PlayerCommandEntry) commandPattern.getLogEntry(), config);
    }

    private static void notifyPlayers(long playerId, PlayerCommandEntry commandEntry, TpaSuspiciousConfig config) {
        StellarProtect plugin = StellarProtect.getInstance();
        String playerName = getName(playerId);

        Function<String, String> replacer = text -> text
            .replace("<player>", playerName)
            .replace("<command>", commandEntry.getCommand())
            .replace("<death>", playerName);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.hasPermission(config.getPermission())) continue;

            plugin.getProtectNMS().sendActionTitle(
                player,
                config.getMessage(),
                config.getTooltip(),
                "/tp " + playerName,
                replacer
            );
        }
    }

    private static TpaSuspiciousConfig getTPAConfig() {
        return (TpaSuspiciousConfig) StellarProtect.getInstance()
            .getConfigManager()
            .getPatternConfig(SuspiciousType.TPA_KILL);
    }

    public static void cacheName(long playerId, String name) {
        playerNames.put(playerId, name);
    }

    public static void removeCacheName(long playerId) {
        playerNames.remove(playerId);
        patterns.remove(playerId);
    }

    public static String getName(long playerId) {
        if (playerId == -1L) {
            return "Console";
        }
        return playerNames.getOrDefault(playerId, PlayerUtils.getEntityType(playerId));
    }

}