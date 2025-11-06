package io.github.insideranh.stellarprotect.api;

import io.github.insideranh.stellarprotect.items.ItemTemplate;

public interface ItemsProvider {

    ItemTemplate getItemTemplate(long templateId);

}

