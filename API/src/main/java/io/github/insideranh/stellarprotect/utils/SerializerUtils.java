package io.github.insideranh.stellarprotect.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.api.ItemsProvider;
import io.github.insideranh.stellarprotect.api.ItemsProviderRegistry;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import lombok.Getter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SerializerUtils {

    @Getter
    private static final Gson gson = new Gson();

    public static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 16);

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
    }

    public static void setInventoryContent(Inventory inventory, JsonObject jsonObject) {
        ItemsProvider itemsProvider = ItemsProviderRegistry.getProvider();
        if (itemsProvider == null) {
            return;
        }

        inventory.clear();

        for (String slotKey : jsonObject.keySet()) {
            try {
                int slot = Integer.parseInt(slotKey);
                if (slot < 0 || slot >= inventory.getSize()) continue;

                JsonObject slotData = jsonObject.getAsJsonObject(slotKey);
                long templateId = slotData.get("t").getAsLong();
                int amount = slotData.get("a").getAsInt();

                ItemTemplate itemTemplate = itemsProvider.getItemTemplate(templateId);
                if (itemTemplate != null) {
                    ItemStack item = itemTemplate.getBukkitItem().clone();
                    item.setAmount(amount);
                    inventory.setItem(slot, item);
                }
            } catch (NumberFormatException | NullPointerException ignored) {
            }
        }
    }

}