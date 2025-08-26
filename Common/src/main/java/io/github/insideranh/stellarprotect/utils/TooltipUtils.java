package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.items.MinecraftItem;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class TooltipUtils {

    private static final StellarProtect plugin = StellarProtect.getInstance();

    public static String getTooltipEnchants(ItemStack item) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();

            builder.append("\n")
                .append(plugin.getLangManager().get("messages.tooltips.item_details.enchant_format")
                    .replace("<name>", enchantment.getName())
                    .replace("<level>", String.valueOf(level))
                );
        }
        return builder.toString().replaceFirst("\n", "");
    }

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