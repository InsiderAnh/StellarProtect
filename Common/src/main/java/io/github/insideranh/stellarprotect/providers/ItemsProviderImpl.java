package io.github.insideranh.stellarprotect.providers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.api.ItemsProvider;
import io.github.insideranh.stellarprotect.items.ItemTemplate;

public class ItemsProviderImpl implements ItemsProvider {

    private final StellarProtect plugin;

    public ItemsProviderImpl(StellarProtect plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemTemplate getItemTemplate(long templateId) {
        return plugin.getItemsManager().getItemTemplate(templateId);
    }

}

