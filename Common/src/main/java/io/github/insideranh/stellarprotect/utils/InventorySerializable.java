package io.github.insideranh.stellarprotect.utils;

import lombok.SneakyThrows;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InventorySerializable {

    @SneakyThrows
    public static String itemStackToBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    @SneakyThrows
    public static ItemStack itemStackFromBase64(String data) {
        if (data == null || data.trim().isEmpty()) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static String createSimpleItemKey(ItemStack item) {
        StringBuilder key = new StringBuilder();
        key.append(item.getType().name());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()) {
                key.append("|name:").append(meta.getDisplayName());
            }

            if (meta.hasEnchants()) {
                key.append("|enchants:").append(meta.getEnchants().toString());
            }

            if (meta.hasLore()) {
                key.append("|lore:").append(String.join(",", meta.getLore()));
            }
        }

        return key.toString();
    }

}
