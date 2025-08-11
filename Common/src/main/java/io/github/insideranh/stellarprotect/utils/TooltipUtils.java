package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.items.MinecraftItem;

import java.util.Map;

public class TooltipUtils {

    private static final StellarProtect plugin = StellarProtect.getInstance();

    public static String getTooltipAdded(Map<Long, Integer> added) {
        StringBuilder builder = new StringBuilder();

        if (!added.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : added.entrySet()) {
                Long key = entry.getKey();
                int value = entry.getValue();

                ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(key);
                if (itemTemplate != null) {
                    MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(itemTemplate.getBukkitItem().getType().name());

                    builder.append("\n")
                        .append(plugin.getLangManager().get("messages.tooltips.added_item")
                            .replace("<data>", minecraftItem.getCleanName())
                            .replace("<amount>", String.valueOf(value))
                        );
                }
            }
        } else {
            builder.append(plugin.getLangManager().get("messages.tooltips.no_changes"));
        }

        return builder.toString().replaceFirst("\n", "");
    }

    public static String getTooltipRemoved(Map<Long, Integer> removed) {
        StringBuilder builder = new StringBuilder();

        if (!removed.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : removed.entrySet()) {
                Long key = entry.getKey();
                int value = entry.getValue();

                ItemTemplate itemTemplate = plugin.getItemsManager().getItemTemplate(key);
                if (itemTemplate != null) {
                    MinecraftItem minecraftItem = StringCleanerUtils.parseMinecraftData(itemTemplate.getBukkitItem().getType().name());

                    builder.append("\n")
                        .append(plugin.getLangManager().get("messages.tooltips.removed_item")
                            .replace("<data>", minecraftItem.getCleanName())
                            .replace("<amount>", String.valueOf(value))
                        );
                }
            }
        } else {
            builder.append(plugin.getLangManager().get("messages.tooltips.no_changes"));
        }

        return builder.toString().replaceFirst("\n", "");
    }

}