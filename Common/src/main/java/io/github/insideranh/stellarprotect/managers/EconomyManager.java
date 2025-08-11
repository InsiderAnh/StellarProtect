package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerEconomyEntry;
import io.github.insideranh.stellarprotect.enums.MoneyVarType;
import io.github.insideranh.stellarprotect.hooks.tasks.TaskCanceller;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private TaskCanceller taskCanceller;

    public void load() {
        if (taskCanceller != null) {
            taskCanceller.cancel();
        }

        if (plugin.getConfigManager().isEconomyDisabled() || plugin.getEconomy() == null) return;

        taskCanceller = plugin.getStellarTaskHook(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline()) continue;
                PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
                if (playerProtect == null) continue;

                double balance = StringCleanerUtils.limitTo2Decimals(plugin.getEconomy().getBalance(player));
                if (balance == playerProtect.getLastEconomyBalance()) continue;

                double difference = balance - playerProtect.getLastEconomyBalance();
                playerProtect.setLastEconomyBalance(balance);

                LoggerCache.addLog(new PlayerEconomyEntry(playerProtect.getPlayerId(), player.getLocation(), MoneyVarType.VAULT, difference));
            }
        }).runTaskTimerAsynchronously(0L, plugin.getConfigManager().getEconomyCheckInterval() * 20L);
    }

}