package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.items.MemoryAnalysisItem;
import io.github.insideranh.stellarprotect.items.memory.ItemTemplateLight;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jol.info.GraphLayout;

import java.util.*;

public class MemoryArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        boolean footprint = arguments.length > 0 && arguments[0].equalsIgnoreCase("footprint");
        sender.sendMessage("§aLoading memory analysis...");

        plugin.getLookupExecutor().execute(() -> {
            Map<String, MemoryAnalysisItem> memoryObjects = new LinkedHashMap<>();

            HashMap<Long, ItemTemplateLight> idToTemplate = new HashMap<>();
            for (ItemTemplate item : plugin.getItemsManager().getItemCache().items()) {
                ItemTemplateLight light = new ItemTemplateLight(item.getId(), item.getBase64(), item.getDisplayName(), item.getLore(), item.getTypeName(), item.getDisplayNameLower(), item.getLoreLower(), item.getTypeNameLower());
                idToTemplate.put(item.getId(), light);
            }

            memoryObjects.put("messages.memory.itemTemplates", new MemoryAnalysisItem(idToTemplate));
            memoryObjects.put("messages.memory.cachedLogsByCategory", new MemoryAnalysisItem(LoggerCache.getCachedLogsByCategory()));
            memoryObjects.put("messages.memory.unSavedLogsByCategory", new MemoryAnalysisItem(LoggerCache.getUnSavedLogsByCategory()));
            memoryObjects.put("messages.memory.placedBlockLogs", new MemoryAnalysisItem(LoggerCache.getPlacedBlockLogs()));
            memoryObjects.put("messages.memory.queryCache", new MemoryAnalysisItem(LoggerCache.getQueryCache()));

            sender.sendMessage(plugin.getLangManager().get("messages.memory.title"));

            long totalMemoryUsage = 0;
            for (Map.Entry<String, MemoryAnalysisItem> entry : memoryObjects.entrySet()) {
                String messageKey = entry.getKey();
                MemoryAnalysisItem item = entry.getValue();

                sender.sendMessage(plugin.getLangManager().get(messageKey));

                if (footprint) {
                    sender.sendMessage(plugin.getLangManager().get("messages.memory.footprint"));
                    sender.sendMessage(GraphLayout.parseInstance(item.getObject()).toFootprint());
                }

                long sizeInBytes = GraphLayout.parseInstance(item.getObject()).totalSize();
                String formattedSize = formatMemorySize(sizeInBytes);

                sender.sendMessage(plugin.getLangManager().get("messages.memory.total") + " " + formattedSize);
                sender.sendMessage("");

                totalMemoryUsage += sizeInBytes;
            }

            sender.sendMessage(plugin.getLangManager().get("messages.memory.summary.title"));
            sender.sendMessage(plugin.getLangManager().get("messages.memory.summary.totalMemory") + formatMemorySize(totalMemoryUsage));
            sender.sendMessage(plugin.getLangManager().get("messages.memory.summary.objectsAnalyzed") + memoryObjects.size());
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new LinkedList<>(Collections.singletonList("footprint"));
    }

    private String formatMemorySize(long bytes) {
        if (bytes < 1024) {
            return "§f" + bytes + " §7bytes";
        }

        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("§f%.2f §7KB §8(§f%d §7bytes§8)", kb, bytes);
        }

        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("§f%.2f §7MB §8(§f%.2f §7KB§8)", mb, kb);
        }

        double gb = mb / 1024.0;
        return String.format("§f%.2f §7GB §8(§f%.2f §7MB§8)", gb, mb);
    }

}