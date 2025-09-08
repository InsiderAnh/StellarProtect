package io.github.insideranh.stellarprotect.hooks.vault;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.economy.PlayerEconomyEntry;
import io.github.insideranh.stellarprotect.enums.MoneyVarType;
import io.github.insideranh.stellarprotect.hooks.tasks.TaskCanceller;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook extends DefaultVaultHook {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private Economy economy;
    private TaskCanceller taskCanceller;

    @Override
    public void load() {
        if (taskCanceller != null) {
            taskCanceller.cancel();
        }

        if (plugin.getConfigManager().isEconomyDisabled() || economy == null) return;

        taskCanceller = plugin.getStellarTaskHook(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline()) continue;
                PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
                if (playerProtect == null) continue;

                double balance = StringCleanerUtils.limitTo2Decimals(economy.getBalance(player));
                if (balance == playerProtect.getLastEconomyBalance()) continue;

                double difference = balance - playerProtect.getLastEconomyBalance();
                playerProtect.setLastEconomyBalance(balance);

                LoggerCache.addLog(new PlayerEconomyEntry(playerProtect.getPlayerId(), player.getLocation(), MoneyVarType.VAULT, difference));
            }
        }).runTaskTimerAsynchronously(0L, plugin.getConfigManager().getEconomyCheckInterval() * 20L);
    }

    @Override
    public void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider == null) {
            return;
        }

        economy = economyProvider.getProvider();
    }

    @Override
    public void joinPlayer(Player player, PlayerProtect playerProtect) {
        if (!plugin.getConfigManager().isEconomyDisabled() && economy != null) {
            playerProtect.setLastEconomyBalance(StringCleanerUtils.limitTo2Decimals(economy.getBalance(player)));
        }
    }
}