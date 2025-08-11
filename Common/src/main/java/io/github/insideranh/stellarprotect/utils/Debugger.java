package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Debugger {

    private final StellarProtect plugin = StellarProtect.getInstance();

    public void debugLog(String message) {
        if (!plugin.getConfigManager().isDebugLog()) return;

        plugin.getLogger().info(message);
    }

    public void debugSave(String message) {
        if (!plugin.getConfigManager().isDebugSave()) return;

        plugin.getLogger().info(message);
    }

    public void debugExtras(String message) {
        if (!plugin.getConfigManager().isDebugExtras()) return;

        plugin.getLogger().info(message);
    }

}