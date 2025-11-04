package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.data.RestoreSession;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.MinecraftItem;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import io.github.insideranh.stellarprotect.utils.TimeUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestoreSessionManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void showRestoreSession(RestoreSession session) {
        Player player = session.getPlayer();

        session.getRestoredLogHashes().clear();
        session.getProcessedLogHashes().clear();

        plugin.getProtectDatabase().getRestoreActions(
            session.getDatabaseFilters(),
            session.getCurrentOffset(),
            session.getLogsPerPage()
        ).thenAccept(callbackLookup -> {
            Map<LocationCache, Set<LogEntry>> groupedLogs = callbackLookup.getLogs();

            if (groupedLogs.isEmpty()) {
                plugin.getLangManager().sendMessage(player, "messages.noMoreLogs");
                return;
            }

            displayLogsToPlayer(session, groupedLogs);

        }).exceptionally(error -> {
            plugin.getLangManager().sendMessage(player, "messages.error");
            error.printStackTrace();
            return null;
        });
    }

    private void displayLogsToPlayer(RestoreSession session, Map<LocationCache, Set<LogEntry>> groupedLogs) {
        Player player = session.getPlayer();

        if (!session.isSilent()) {
            player.sendMessage(plugin.getLangManager().get("messages.sessions.title"));
            player.sendMessage(plugin.getLangManager().get("messages.sessions.subtitle"));
            player.sendMessage("");
        }

        List<LogEntry> allLogs = new ArrayList<>();
        for (Set<LogEntry> logs : groupedLogs.values()) {
            allLogs.addAll(logs);
        }

        allLogs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        for (LogEntry log : allLogs) {
            int logHash = log.hashCode();

            if (session.isProcessed(logHash)) {
                continue;
            }

            session.addProcessedLog(logHash, log);
            displayLogEntry(session, log, logHash);
        }

        displayNavigationButtons(session);

        session.nextPage();
    }

    private void displayLogEntry(RestoreSession session, LogEntry logEntry, int logHash) {
        Player player = session.getPlayer();
        boolean isRestored = session.isRestored(logHash);

        ActionType actionType = ActionType.getById(logEntry.getActionType());
        if (actionType == null) return;

        String action = actionType.name().toLowerCase();

        String data;
        if (actionType.isParseMinecraftData()) {
            MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(logEntry.getDataString());
            data = minecraftItem.getCleanName();
        } else {
            data = logEntry.getDataString();
        }

        TextComponent message = new TextComponent();

        TextComponent actionButton;
        if (isRestored) {
            actionButton = new TextComponent("§8[§cX§8] ");
            actionButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/stellarprotect rs undo " + logHash));
            actionButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(plugin.getLangManager().get("messages.sessions.undo"))));
        } else {
            actionButton = new TextComponent("§8[§a<<§8] ");
            actionButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/stellarprotect rs restore " + logHash));
            actionButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(plugin.getLangManager().get("messages.sessions.restore"))));
        }

        message.addExtra(actionButton);
        message.addExtra(new TextComponent(plugin.getLangManager().get("messages.sessions." + action,
            text -> text
                .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                .replace("<player>", PlayerUtils.getNameOfEntity(logEntry.getPlayerId()))
                .replace("<data>", data)
        )));
        player.spigot().sendMessage(message);
    }

    private void displayNavigationButtons(RestoreSession session) {
        Player player = session.getPlayer();

        player.sendMessage("");

        TextComponent restoreAllButton = new TextComponent(plugin.getLangManager().get("messages.sessions.restoreall"));
        restoreAllButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect rs restoreall"));
        restoreAllButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.sessions.restoreall-tip"))));

        TextComponent nextPageButton = new TextComponent(plugin.getLangManager().get("messages.sessions.next"));
        nextPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect rs next"));
        nextPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.sessions.next-tip"))));

        TextComponent exitButton = new TextComponent(plugin.getLangManager().get("messages.sessions.exit"));
        exitButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect rs exit"));
        exitButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.sessions.exit-tip"))));

        TextComponent navigationMessage = new TextComponent("");
        navigationMessage.addExtra(restoreAllButton);
        navigationMessage.addExtra(nextPageButton);
        navigationMessage.addExtra(exitButton);

        player.spigot().sendMessage(navigationMessage);
        player.sendMessage("");
    }

    public void restoreIndividualLog(RestoreSession session, int logHash) {
        LogEntry log = (LogEntry) session.getProcessedLog(logHash);
        if (log == null) {
            plugin.getLangManager().sendMessage(session.getPlayer(), "messages.invalidLog");
            return;
        }
        if (!session.isRestored(logHash)) {
            plugin.getRestoreManager().restore(log, session.getPlayer(), session.isVerbose());
        }

        session.markAsRestored(logHash, log);
        log.setRestored(true);

        if (!session.isSilent()) {
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.sessions.restored"));
        }

        showRestoreSession(session);
    }

    public void undoIndividualRestore(RestoreSession session, int logHash) {
        LogEntry log = (LogEntry) session.getProcessedLog(logHash);
        if (log == null) {
            plugin.getLangManager().sendMessage(session.getPlayer(), "messages.invalidLog");
            return;
        }
        session.unmarkRestored(logHash);
        log.setRestored(false);

        if (!session.isSilent()) {
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.sessions.undone"));
        }

        showRestoreSession(session);
    }

    public void restoreAllVisible(RestoreSession session) {
        int restoredCount = 0;

        for (Map.Entry<Integer, Object> entry : session.getProcessedLogHashes().entrySet()) {
            int logHash = entry.getKey();
            LogEntry log = (LogEntry) entry.getValue();
            if (!session.isRestored(logHash)) {
                plugin.getRestoreManager().restore(log, session.getPlayer(), session.isVerbose());
                session.markAsRestored(logHash, log);
                restoredCount++;
            }
        }

        if (!session.isSilent()) {
            int finalRestoredCount = restoredCount;
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.sessions.amount-restored", replace -> replace.replace("<amount>", String.valueOf(finalRestoredCount))));
        }

        showRestoreSession(session);
    }

    public void nextPage(RestoreSession session) {
        showRestoreSession(session);
    }

    public void exitSession(RestoreSession session) {
        Player player = session.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);

        if (playerProtect != null) {
            playerProtect.setRestoreSession(null);
        }

        if (!session.isSilent()) {
            int totalRestored = session.getRestoredLogHashes().size();
            player.sendMessage(String.format("§eSesión de restauración finalizada. Total restaurado: §f%d §elogs", totalRestored));
        }
    }

}
