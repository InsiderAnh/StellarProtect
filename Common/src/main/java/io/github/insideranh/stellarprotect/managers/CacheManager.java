package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.BlockSourceCache;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.enums.ActionCategory;
import io.github.insideranh.stellarprotect.hooks.tasks.TaskCanceller;

import java.util.concurrent.ThreadLocalRandom;

public class CacheManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private TaskCanceller saveTask;
    private TaskCanceller deleteOldTask;
    private TaskCanceller saveItemTask;
    private TaskCanceller blockSaveTask;

    public void load() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (deleteOldTask != null) {
            deleteOldTask.cancel();
        }
        if (saveItemTask != null) {
            saveItemTask.cancel();
        }
        if (blockSaveTask != null) {
            blockSaveTask.cancel();
        }

        saveTask = plugin.getStellarTaskHook(() -> {
            plugin.getProtectDatabase().save(LoggerCache.getFlushLogsToDatabase());
            LoggerCache.clearRamCache();
        }).runTaskTimerAsynchronously(plugin.getConfigManager().getSavePeriod() * 20L, plugin.getConfigManager().getSavePeriod() * 20L);
        deleteOldTask = plugin.getStellarTaskHook(() -> {
            plugin.getProtectDatabase().clearOldLogs();
            plugin.getChestTransactionTracker().cleanupOldStates();
            BlockSourceCache.cleanup();
        }).runTaskTimerAsynchronously(plugin.getConfigManager().getDeleteOldPeriod() * 20L, plugin.getConfigManager().getDeleteOldPeriod() * 20L);
        saveItemTask = plugin.getStellarTaskHook(() -> plugin.getItemsManager().saveItems()).runTaskTimerAsynchronously(plugin.getConfigManager().getSaveItemPeriod() + ThreadLocalRandom.current().nextInt(0, 5) * 20L, plugin.getConfigManager().getSaveItemPeriod() * 20L);
        blockSaveTask = plugin.getStellarTaskHook(() -> plugin.getBlocksManager().saveBlocks()).runTaskTimerAsynchronously(plugin.getConfigManager().getSaveBlockPeriod() + ThreadLocalRandom.current().nextInt(0, 5) * 20L, plugin.getConfigManager().getSaveBlockPeriod() * 20L);
    }

    public void forceSave(ActionCategory actionCategory) {
        plugin.getProtectDatabase().save(LoggerCache.getFlushLogsToDatabase(actionCategory));
    }

}