package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.items.ItemTemplate;

import java.util.List;

public interface ItemsRepository {

    void saveItems(List<ItemTemplate> itemTemplates);

    void updateItemUsageInDatabase(long templateId, int quantity);

    void loadMostUsedItems();

}