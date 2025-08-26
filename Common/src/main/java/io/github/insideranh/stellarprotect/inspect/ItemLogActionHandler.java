package io.github.insideranh.stellarprotect.inspect;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.LogEntry;
import io.github.insideranh.stellarprotect.database.entries.players.PlayerItemLogEntry;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import io.github.insideranh.stellarprotect.utils.TimeUtils;
import io.github.insideranh.stellarprotect.utils.TooltipUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemLogActionHandler implements InspectHandler.ActionHandler {

    @Override
    public void handle(Player player, LogEntry logEntry, StellarProtect plugin) {
        PlayerItemLogEntry itemLogEntry = (PlayerItemLogEntry) logEntry;
        ActionType actionType = ActionType.getById(itemLogEntry.getActionType());
        if (actionType == null) return;

        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ItemDetails itemDetails = buildItemDetails(itemLogEntry, plugin);
        String tooltipBody = buildTooltipBody(itemDetails, plugin);

        playerProtect.getPosibleLogs().put(itemLogEntry.hashCode(), itemLogEntry);

        sendActionTitle(player, itemDetails, tooltipBody, logEntry, actionType, plugin);
    }

    private ItemDetails buildItemDetails(PlayerItemLogEntry itemLogEntry, StellarProtect plugin) {
        ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(itemLogEntry.getItemReferenceId());
        ItemStack item = itemTemplate.getBukkitItem();
        String cleanName = StringCleanerUtils.parseMinecraftData(item.getType().name()).getCleanName();

        return new ItemDetails(item, itemLogEntry.getAmount(), cleanName);
    }

    private String buildTooltipBody(ItemDetails itemDetails, StellarProtect plugin) {
        List<String> bodyLines = new ArrayList<>();
        List<String> templateLines = plugin.getLangManager().getList("messages.tooltips.item_details.body");

        for (String line : templateLines) {
            bodyLines.addAll(processTemplateLine(line, itemDetails, plugin));
        }

        return String.join("\n", bodyLines);
    }

    private List<String> processTemplateLine(String line, ItemDetails itemDetails, StellarProtect plugin) {
        ItemStack item = itemDetails.getItem();

        if (line.contains("<display_name>")) {
            if (hasDisplayName(item)) {
                return replaceTemplate("displayName", item.getItemMeta().getDisplayName(), plugin);
            } else {
                return new LinkedList<>();
            }
        }

        if (line.contains("<lore>")) {
            if (hasLore(item)) {
                String joinedLore = String.join("\n", item.getItemMeta().getLore());
                return replaceTemplate("lore", joinedLore, plugin);
            } else {
                return new LinkedList<>();
            }
        }

        if (line.contains("<enchants>")) {
            if (hasEnchants(item)) {
                return replaceTemplate("enchants", TooltipUtils.getTooltipEnchants(item), plugin);
            } else {
                return new LinkedList<>();
            }
        }

        return new LinkedList<>(Collections.singletonList(line.replace("<amount>", String.valueOf(itemDetails.getAmount()))));
    }

    private List<String> replaceTemplate(String templateType, String value, StellarProtect plugin) {
        String langKey = "messages.tooltips.item_details." + templateType;
        String placeholder = "<" + templateType + ">";

        return plugin.getLangManager().getList(langKey).stream()
            .map(template -> template.replace(placeholder, value))
            .collect(Collectors.toList());
    }

    private void sendActionTitle(Player player, ItemDetails itemDetails, String tooltipBody, LogEntry logEntry, ActionType actionType, StellarProtect plugin) {
        String actionKey = actionType.name().toLowerCase();

        plugin.getProtectNMS().sendActionTitle(player,
            plugin.getLangManager().get("messages.actions." + actionKey),
            tooltipBody,
            "/spt view item " + logEntry.hashCode(),
            text -> text
                .replace("<time>", TimeUtils.formatMillisAsAgo(logEntry.getCreatedAt()))
                .replace("<player>", PlayerCache.getName(logEntry.getPlayerId()))
                .replace("<data>", itemDetails.getCleanName())
        );
    }

    private static boolean hasDisplayName(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private static boolean hasLore(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasLore();
    }

    private static boolean hasEnchants(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasEnchants();
    }

    private static class ItemDetails {

        private final ItemStack item;
        private final int amount;
        private final String cleanName;

        ItemDetails(ItemStack item, int amount, String cleanName) {
            this.item = item;
            this.amount = amount;
            this.cleanName = cleanName;
        }

        ItemStack getItem() {
            return item;
        }

        int getAmount() {
            return amount;
        }

        String getCleanName() {
            return cleanName;
        }

    }

}