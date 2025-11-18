package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.keys.LocationCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.data.UndoSession;
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

public class UndoSessionManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void showUndoSession(UndoSession session) {
        Player player = session.getPlayer();

        session.getUndoneLogHashes().clear();
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

    private void displayLogsToPlayer(UndoSession session, Map<LocationCache, Set<LogEntry>> groupedLogs) {
        Player player = session.getPlayer();

        if (!session.isSilent()) {
            player.sendMessage(plugin.getLangManager().get("messages.undo.sessions.title"));
            player.sendMessage(plugin.getLangManager().get("messages.undo.sessions.subtitle"));
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

    private void displayLogEntry(UndoSession session, LogEntry logEntry, int logHash) {
        Player player = session.getPlayer();
        boolean isUndone = session.isUndone(logHash);

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
        if (isUndone) {
            actionButton = new TextComponent("§8[§cX§8] ");
            actionButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/stellarprotect us redo " + logHash));
            actionButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(plugin.getLangManager().get("messages.undo.sessions.redo"))));
        } else {
            actionButton = new TextComponent("§8[§e>>§8] ");
            actionButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/stellarprotect us undo " + logHash));
            actionButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(plugin.getLangManager().get("messages.undo.sessions.undo"))));
        }

        message.addExtra(actionButton);
        message.addExtra(new TextComponent(plugin.getLangManager().get("messages.undo.sessions." + action,
            text -> text
                .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                .replace("<player>", PlayerUtils.getNameOfEntity(logEntry.getPlayerId()))
                .replace("<data>", data)
        )));
        player.spigot().sendMessage(message);
    }

    private void displayNavigationButtons(UndoSession session) {
        Player player = session.getPlayer();

        player.sendMessage("");

        TextComponent undoAllButton = new TextComponent(plugin.getLangManager().get("messages.undo.sessions.undoall"));
        undoAllButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect us undoall"));
        undoAllButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.undo.sessions.undoall-tip"))));

        TextComponent nextPageButton = new TextComponent(plugin.getLangManager().get("messages.undo.sessions.next"));
        nextPageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect us next"));
        nextPageButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.undo.sessions.next-tip"))));

        TextComponent exitButton = new TextComponent(plugin.getLangManager().get("messages.undo.sessions.exit"));
        exitButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/stellarprotect us exit"));
        exitButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText(plugin.getLangManager().get("messages.undo.sessions.exit-tip"))));

        TextComponent navigationMessage = new TextComponent("");
        navigationMessage.addExtra(undoAllButton);
        navigationMessage.addExtra(nextPageButton);
        navigationMessage.addExtra(exitButton);

        player.spigot().sendMessage(navigationMessage);
        player.sendMessage("");
    }

    public void undoIndividualLog(UndoSession session, int logHash) {
        LogEntry log = (LogEntry) session.getProcessedLog(logHash);
        if (log == null) {
            plugin.getLangManager().sendMessage(session.getPlayer(), "messages.invalidLog");
            return;
        }
        if (!session.isUndone(logHash)) {
            plugin.getUndoManager().undoRestore(log, session.getPlayer(), session.isVerbose());
        }

        session.markAsUndone(logHash, log);

        if (!session.isSilent()) {
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.undo.sessions.undone"));
        }

        showUndoSession(session);
    }

    public void redoIndividualLog(UndoSession session, int logHash) {
        LogEntry log = (LogEntry) session.getProcessedLog(logHash);
        if (log == null) {
            plugin.getLangManager().sendMessage(session.getPlayer(), "messages.invalidLog");
            return;
        }

        if (session.isUndone(logHash)) {
            plugin.getRestoreManager().restore(log, session.getPlayer(), session.isVerbose());
        }

        session.unmarkUndone(logHash);

        if (!session.isSilent()) {
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.undo.sessions.redone"));
        }

        showUndoSession(session);
    }

    public void undoAllLogs(UndoSession session) {
        session.getProcessedLogHashes().forEach((hash, logEntry) -> {
            if (!session.isUndone(hash)) {
                plugin.getUndoManager().undoRestore((LogEntry) logEntry, session.getPlayer(), session.isVerbose());
                session.markAsUndone(hash, logEntry);
            }
        });

        if (!session.isSilent()) {
            session.getPlayer().sendMessage(plugin.getLangManager().get("messages.undo.sessions.undoneall"));
        }

        showUndoSession(session);
    }

    public void exitSession(UndoSession session) {
        Player player = session.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect != null) {
            playerProtect.setUndoSession(null);
        }

        if (!session.isSilent()) {
            plugin.getLangManager().sendMessage(player, "messages.undo.sessions.exited");
        }
    }

}