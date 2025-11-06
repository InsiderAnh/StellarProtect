package io.github.insideranh.stellarprotect.api;

import lombok.Getter;

public class ItemsProviderRegistry {

    @Getter
    private static ItemsProvider provider;

    public static void register(ItemsProvider provider) {
        ItemsProviderRegistry.provider = provider;
    }

    public static boolean isRegistered() {
        return provider != null;
    }

}

