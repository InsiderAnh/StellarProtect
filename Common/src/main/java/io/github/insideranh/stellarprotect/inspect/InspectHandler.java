package io.github.insideranh.stellarprotect.inspect;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerEconomyEntry;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerXPEntry;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerFurnitureLogEntry;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerXKitEventLogEntry;
import io.github.insideranh.stellarprotect.database.entries.items.ItemLogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.*;
import io.github.insideranh.stellarprotect.database.entries.world.CropGrowLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.items.MinecraftItem;
import io.github.insideranh.stellarprotect.utils.LocationUtils;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import io.github.insideranh.stellarprotect.utils.TimeUtils;
import io.github.insideranh.stellarprotect.utils.TooltipUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InspectHandler {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void handleChestInspection(Player player, Location blockLocation, int page, int skip, int limit) {
        plugin.getProtectDatabase().getChestTransactions(blockLocation, skip, limit).thenAccept(callbackLookup -> {
            int maxPage = (int) Math.ceil((double) callbackLookup.getTotal() / limit);

            List<ItemLogEntry> logs = callbackLookup.getLogs();
            if (logs.isEmpty()) {
                sendNoLogsMessage(player, blockLocation);
                return;
            }

            plugin.getLangManager().sendMessage(player, "messages.actions.transactions_title",
                text -> text.replace("<location>", LocationUtils.getFormattedStringLocation(blockLocation)));

            logs.forEach(transaction -> processItemTransaction(player, transaction));
            sendPaginationInfo(player, page, limit, maxPage);
        });
    }

    public void handleBlockInspection(Player player, Location blockLocation, int page, int skip, int limit) {
        plugin.getProtectDatabase().getLogs(blockLocation, skip, limit).thenAccept(callbackLookup -> {
            int maxPage = (int) Math.ceil((double) callbackLookup.getTotal() / limit);
            Set<LogEntry> logs = callbackLookup.getLogs();
            if (logs.isEmpty()) {
                sendNoLogsMessage(player, blockLocation);
                return;
            }

            plugin.getLangManager().sendMessage(player, "messages.actions.location",
                text -> text.replace("<location>", LocationUtils.getFormattedStringLocation(blockLocation)));

            logs.forEach(logEntry -> processLogEntry(player, logEntry));
            sendPaginationInfo(player, page, limit, maxPage);
        });
    }

    public void processItemTransaction(Player player, ItemLogEntry transaction) {
        ItemStack item = transaction.getItemStack();
        if (item == null) return;

        String messageKey = transaction.isAdded() ? "messages.actions.added_item" : "messages.actions.removed_item";

        MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(item.getType().name());

        plugin.getLangManager().sendMessage(player, messageKey,
            text -> text
                .replace("<time>", TimeUtils.formatMillisAsAgo(transaction.getCreatedAt()))
                .replace("<player>", PlayerCache.getName(transaction.getPlayerId()))
                .replace("<data>", minecraftItem.getCleanName())
                .replace("<amount>", String.valueOf(transaction.getAmount()))
        );
    }

    public void processLogEntry(Player player, LogEntry logEntry) {
        if (logEntry == null) return;

        ActionType actionType = ActionType.getById(logEntry.getActionType());
        if (actionType == null) return;

        InspectHandler.ActionHandler handler = InspectHandler.ActionHandlerFactory.getHandler(actionType);
        if (handler != null) {
            handler.handle(player, logEntry, plugin);
        } else {
            handleGenericAction(player, logEntry, actionType);
        }
    }

    public void handleGenericAction(Player player, LogEntry logEntry, ActionType actionType) {
        String data;
        if (actionType.isParseMinecraftData()) {
            MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(logEntry.getDataString());
            data = minecraftItem.getCleanName();
        } else {
            data = logEntry.getDataString();
        }

        plugin.getLangManager().sendMessage(player, "messages.actions." + actionType.name().toLowerCase(),
            text -> text
                .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                .replace("<data>", data)
        );
    }

    public void sendNoLogsMessage(Player player, Location blockLocation) {
        plugin.getLangManager().sendMessage(player, "messages.noInspectLogs",
            text -> text.replace("<location>", LocationUtils.getFormattedStringLocation(blockLocation)));
    }

    public void sendPaginationInfo(Player player, int currentPage, int itemsPerPage, long total) {
        plugin.getProtectNMS().sendPageButtons(player,
            plugin.getLangManager().get("messages.pagesNav"),
            plugin.getLangManager().get("messages.clickPage"),
            currentPage,
            itemsPerPage,
            (int) total
        );
    }

    public interface ActionHandler {
        void handle(Player player, LogEntry logEntry, StellarProtect plugin);
    }

    public static class ActionHandlerFactory {

        private static final Map<ActionType, ActionHandler> handlers = new HashMap<>();

        static {
            handlers.put(ActionType.SESSION, new SessionActionHandler());
            handlers.put(ActionType.SIGN_CHANGE, new SignChangeActionHandler());
            handlers.put(ActionType.INVENTORY_TRANSACTION, new InventoryTransactionActionHandler());

            handlers.put(ActionType.MOUNT, new MountActionHandler());
            handlers.put(ActionType.SHOOT, new ShootActionHandler());
            handlers.put(ActionType.GAME_MODE, new GameModeActionHandler());
            handlers.put(ActionType.XP, new XPActionHandler());
            handlers.put(ActionType.MONEY, new EconomyActionHandler());

            PlaceRemoveItemActionHandler placeRemoveActionHandler = new PlaceRemoveItemActionHandler();
            handlers.put(ActionType.PLACE_ITEM, placeRemoveActionHandler);
            handlers.put(ActionType.REMOVE_ITEM, placeRemoveActionHandler);

            handlers.put(ActionType.CROP_GROW, new GrowAgeActionHandler());

            ItemLogActionHandler itemLogActionHandler = new ItemLogActionHandler();
            handlers.put(ActionType.DROP_ITEM, itemLogActionHandler);
            handlers.put(ActionType.PICKUP_ITEM, itemLogActionHandler);

            FurnitureActionHandler furnitureActionHandler = new FurnitureActionHandler();
            handlers.put(ActionType.FURNITURE_BREAK, furnitureActionHandler);
            handlers.put(ActionType.FURNITURE_PLACE, furnitureActionHandler);

            handlers.put(ActionType.X_KIT_EVENT, new XKitEventActionHandler());
        }

        public static ActionHandler getHandler(ActionType actionType) {
            return handlers.get(actionType);
        }

    }

    public static class FurnitureActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerFurnitureLogEntry furnitureEntry = (PlayerFurnitureLogEntry) logEntry;

            String messageKey = "messages.actions." + (furnitureEntry.getActionType() == ActionType.FURNITURE_BREAK.getId() ? "furniture_break" : "furniture_place");
            plugin.getLangManager().sendMessage(player, messageKey,
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<data>", furnitureEntry.getNexoBlockId())
            );
        }

    }

    public static class XKitEventActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerXKitEventLogEntry eventEntry = (PlayerXKitEventLogEntry) logEntry;

            String messageKey = "messages.actions." + (eventEntry.getEventType() == (byte) 0 ? "x_kit_claim" : "x_kit_give");
            plugin.getLangManager().sendMessage(player, messageKey,
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<data>", eventEntry.getKitId())
            );
        }

    }

    public static class GrowAgeActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            CropGrowLogEntry growAgeEntry = (CropGrowLogEntry) logEntry;
            plugin.getLangManager().sendMessage(player, "messages.actions.crop_grow",
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<data>", plugin.getLangManager().get("messages.actions.growAge", replace -> replace.replace("<age>", String.valueOf(growAgeEntry.getAge()))))
            );
        }

    }

    public static class XPActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerXPEntry xpEntry = (PlayerXPEntry) logEntry;
            plugin.getLangManager().sendMessage(player, "messages.actions." + (xpEntry.getDifference() > 0 ? "add_xp" : "remove_xp"),
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<amount>", String.valueOf(xpEntry.getDifference()))
            );
        }

    }

    public static class EconomyActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerEconomyEntry economyEntry = (PlayerEconomyEntry) logEntry;
            String messageKey = economyEntry.getVariationType().name().toLowerCase() + "_" + (economyEntry.getDifference() > 0 ? "income" : "outcome");
            plugin.getLangManager().sendMessage(player, "messages.actions." + messageKey,
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<amount>", StringCleanerUtils.formatEconomy(economyEntry.getDifference()))
            );
        }

    }

    public static class InventoryTransactionActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerTransactionEntry inventoryTransactionEntry = (PlayerTransactionEntry) logEntry;
            PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
            if (playerProtect == null) return;

            List<String> tooltipBody = new LinkedList<>();
            for (String line : plugin.getLangManager().getList("messages.tooltips.transactions_inventory.body")) {
                if (line.contains("<added>")) {
                    if (!inventoryTransactionEntry.getAdded().isEmpty()) {
                        for (String tooltip : plugin.getLangManager().getList("messages.tooltips.transactions_inventory.added")) {
                            tooltipBody.add(tooltip.replace("<added>", TooltipUtils.getTooltipRemoved(inventoryTransactionEntry.getAdded())));
                        }
                    }
                } else if (line.contains("<removed>")) {
                    if (!inventoryTransactionEntry.getRemoved().isEmpty()) {
                        for (String tooltip : plugin.getLangManager().getList("messages.tooltips.transactions_inventory.removed")) {
                            tooltipBody.add(tooltip.replace("<removed>", TooltipUtils.getTooltipRemoved(inventoryTransactionEntry.getRemoved())));
                        }
                    }
                } else {
                    tooltipBody.add(line);
                }
            }

            playerProtect.getPosibleLogs().put(inventoryTransactionEntry.hashCode(), inventoryTransactionEntry);

            plugin.getProtectNMS().sendActionTitle(player,
                plugin.getLangManager().get("messages.actions.inventory_transaction"),
                String.join("\n", tooltipBody),
                "/spt view inventory " + logEntry.hashCode(),
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<added>", String.valueOf(inventoryTransactionEntry.getAdded().size()))
                    .replace("<removed>", String.valueOf(inventoryTransactionEntry.getRemoved().size()))
            );
        }
    }

    public static class SessionActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerSessionEntry sessionEntry = (PlayerSessionEntry) logEntry;
            if (sessionEntry.getLogin() == 1) {
                plugin.getLangManager().sendMessage(player, "messages.actions.login_session",
                    text -> text
                        .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                        .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                );
            } else {
                plugin.getProtectNMS().sendActionTitle(player,
                    plugin.getLangManager().get("messages.actions.logout_session"),
                    plugin.getLangManager().get("messages.tooltips.logout_session")
                        .replace("<time>", TimeUtils.formatMillisAsCompactDHMS(sessionEntry.getSessionTime() * 1000L)),
                    "",
                    text -> text
                        .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                        .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                );
            }
        }

    }

    public static class PlaceRemoveItemActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerPlaceRemoveItemLogEntry itemEntry = (PlayerPlaceRemoveItemLogEntry) logEntry;
            MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(itemEntry.getDataString());
            ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(itemEntry.getItemReferenceId());
            MinecraftItem explainedItem = StringCleanerUtils.parseMinecraftData(itemTemplate.getBukkitItem().getType().name());
            String actionKey = "messages.actions." + (itemEntry.isPlaced() ? "place_item" : "remove_item");
            String tooltipKey = "messages.tooltips." + (itemEntry.isPlaced() ? "place_item" : "remove_item");

            plugin.getProtectNMS().sendActionTitle(player,
                plugin.getLangManager().get(actionKey),
                plugin.getLangManager().get(tooltipKey, text -> text
                    .replace("<data>", explainedItem.getCleanName())
                    .replace("<amount>", String.valueOf(itemEntry.getAmount()))),
                "",
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<data>", minecraftItem.getCleanName())
            );
        }
    }

    public static class ShootActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(logEntry.getDataString());

            PlayerShootEntry sessionEntry = (PlayerShootEntry) logEntry;
            String login = sessionEntry.getSuccess() == 1 ? "success_shoot" : "shoot";
            plugin.getLangManager().sendMessage(player, "messages.actions." + login,
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<shoot>", sessionEntry.getShootEntityType())
                    .replace("<data>", minecraftItem.getCleanName())
            );
        }

    }

    public static class MountActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(logEntry.getDataString());

            PlayerMountEntry sessionEntry = (PlayerMountEntry) logEntry;
            String login = sessionEntry.getMount() == 1 ? "dismount" : "mount";
            plugin.getLangManager().sendMessage(player, "messages.actions." + login,
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<data>", minecraftItem.getCleanName())
            );
        }

    }

    public static class GameModeActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerGameModeLogEntry gameModeEntry = (PlayerGameModeLogEntry) logEntry;

            String newGameMode = plugin.getLangManager().get("game_modes." + gameModeEntry.getNewGameMode());
            String lastGameMode = plugin.getLangManager().get("game_modes." + gameModeEntry.getLastGameMode());

            plugin.getProtectNMS().sendActionTitle(player,
                plugin.getLangManager().get("messages.actions.game_mode"),
                plugin.getLangManager().get("messages.tooltips.game_mode"),
                "",
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<lastGameMode>", lastGameMode)
                    .replace("<newGameMode>", newGameMode)
            );
        }

    }

    public static class SignChangeActionHandler implements ActionHandler {

        @Override
        public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
            PlayerSignChangeEntry signEntry = (PlayerSignChangeEntry) logEntry;
            plugin.getProtectNMS().sendActionTitle(player,
                plugin.getLangManager().get("messages.actions.sign_change"),
                plugin.getLangManager().get("messages.tooltips.sign_change"),
                "",
                text -> text
                    .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                    .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                    .replace("<line1>", signEntry.getLine(0))
                    .replace("<line2>", signEntry.getLine(1))
                    .replace("<line3>", signEntry.getLine(2))
                    .replace("<line4>", signEntry.getLine(3))
            );
        }

    }

}