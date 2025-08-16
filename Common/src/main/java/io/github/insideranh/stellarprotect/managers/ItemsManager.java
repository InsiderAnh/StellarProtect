package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.items.ItemReference;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ItemsManager {

    private final ConcurrentHashMap<String, Long> itemHashToId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ItemTemplate> idToTemplate = new ConcurrentHashMap<>();
    private final HashSet<Long> unsavedTemplates = new HashSet<>();
    private final StellarProtect plugin = StellarProtect.getInstance();
    private final AtomicLong currentId = new AtomicLong(0);

    public void load() {
        String selectedLang = plugin.getConfigManager().getItemsLang();
        if (selectedLang.equalsIgnoreCase("en")) {
            StringCleanerUtils.setHasTranslation(false);
            return;
        }

        plugin.getLogger().info("Loading items translations...");

        File folder = new File(plugin.getDataFolder(), "translations");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, "items_" + selectedLang + ".yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("translations/items_" + selectedLang + ".yml", false);
                plugin.getLogger().info("Saved default items translations file for " + selectedLang + ".");
            } catch (Exception exception) {
                plugin.getLogger().info("The translation file for " + selectedLang + " does not exist. Using default en, if you need a translation, please contact InsiderAnh on Discord.");
                return;
            }
        }

        StringCleanerUtils.setHasTranslation(true);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            StringCleanerUtils.addMaterialTranslation(key.toUpperCase(), config.getString(key));
        }
        plugin.getLogger().info("Loaded " + keys.size() + " items translations for " + selectedLang + ".");
    }

    public void loadItemReference(ItemTemplate template, String fullBase64) {
        itemHashToId.put(fullBase64, template.getId());
        idToTemplate.put(template.getId(), template);

        currentId.set(idToTemplate.size() + 1L);
    }

    @NonNull
    public ItemReference getItemReference(ItemStack itemStack) {
        return getItemReference(itemStack, itemStack.getAmount());
    }

    @NonNull
    public ItemReference getItemReference(ItemStack itemStack, int amount) {
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            return new ItemReference(-1, 1);
        }

        ItemStack reduced = itemStack.clone();
        reduced.setAmount(1);

        String base64 = InventorySerializable.itemStackToBase64(reduced).replace("\n", "").replace("\r", "");
        Long templateId = itemHashToId.get(base64);
        if (templateId != null) {
            return new ItemReference(templateId, amount);
        }

        templateId = createItemTemplate(itemStack, base64);
        return new ItemReference(templateId, amount);
    }

    public ItemTemplate getItemTemplate(long id) {
        return idToTemplate.get(id);
    }

    public long createItemTemplate(ItemStack itemStack, String base64) {
        long id = currentId.getAndIncrement();

        ItemTemplate template = new ItemTemplate(id, itemStack, base64);
        unsavedTemplates.add(id);
        idToTemplate.put(id, template);
        itemHashToId.put(base64, id);
        return id;
    }

    public void saveItems() {
        if (unsavedTemplates.isEmpty()) return;

        List<ItemTemplate> templates = new ArrayList<>();
        unsavedTemplates.forEach(id -> templates.add(idToTemplate.get(id)));
        unsavedTemplates.clear();
        plugin.getProtectDatabase().saveItems(templates);
    }

    public long getItemReferenceCount() {
        return idToTemplate.size();
    }

}