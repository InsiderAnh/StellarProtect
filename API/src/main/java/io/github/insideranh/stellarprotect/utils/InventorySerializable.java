package io.github.insideranh.stellarprotect.utils;

import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class InventorySerializable {

    @SneakyThrows
    public static String itemStackToBase64(ItemStack item) {
        if (item == null) return null;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return itemStackToBase64Fallback(item);
        }
    }

    @SneakyThrows
    public static ItemStack itemStackFromBase64(String data) {
        if (data == null || data.trim().isEmpty() || data.equals("null")) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (ClassNotFoundException e) {
            return itemStackFromBase64Fallback(data);
        }
    }

    @SneakyThrows
    public static String itemStackToBase64Fallback(ItemStack item) {
        if (item == null) return null;

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("item", item);
            String yaml = config.saveToString();
            return Base64.getEncoder().encodeToString(yaml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack itemStackFromBase64Fallback(String data) {
        if (data == null || data.trim().isEmpty() || data.equals("null")) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(data);
            String yaml = new String(decodedBytes, StandardCharsets.UTF_8);

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            Object item = config.get("item");
            if (item instanceof ItemStack) {
                return (ItemStack) item;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

}
