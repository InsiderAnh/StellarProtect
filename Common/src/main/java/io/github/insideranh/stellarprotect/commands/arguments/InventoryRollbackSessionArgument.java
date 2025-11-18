package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.InventoryRollbackSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Comando para controlar la sesión activa de rollback de inventarios
 * Uso: /sp irs toggle - Activa/desactiva el modo
 * Uso: /sp irs stop - Detiene la sesión completamente
 */
public class InventoryRollbackSessionArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().sendMessage(sender, "messages.onlyPlayer");
            return;
        }
        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        InventoryRollbackSession session = playerProtect.getInventoryRollbackSession();
        if (session == null) {
            plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.noActiveSession");
            return;
        }

        if (arguments.length == 0) {
            // Mostrar estado actual
            showSessionStatus(player, session);
            return;
        }

        String subCommand = arguments[0].toLowerCase();
        switch (subCommand) {
            case "toggle":
                boolean isActive = plugin.getChestRollbackSessionManager().toggleInventoryRollbackMode(player);
                if (isActive) {
                    plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.modeEnabled");
                } else {
                    plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.modeDisabled");
                }
                break;

            case "stop":
            case "cancel":
            case "exit":
                playerProtect.setInventoryRollbackSession(null);
                plugin.getLangManager().sendMessage(player, "messages.inventoryRollback.sessionStopped");
                break;

            case "status":
            case "info":
                showSessionStatus(player, session);
                break;

            default:
                player.sendMessage("§cUso: /sp irs <toggle|stop|status>");
                break;
        }
    }

    private void showSessionStatus(Player player, InventoryRollbackSession session) {
        player.sendMessage("§7§m----------------------------------");
        player.sendMessage("§6Sesión de Rollback de Inventarios");
        player.sendMessage("§7Estado: " + (session.isActive() ? "§aActivo" : "§cInactivo"));
        player.sendMessage("§7Verbose: " + (session.isVerbose() ? "§aSí" : "§cNo"));
        player.sendMessage("§7Silent: " + (session.isSilent() ? "§aSí" : "§cNo"));

        if (session.getDatabaseFilters().getTimeFilter() != null) {
            player.sendMessage("§7Tiempo: §f" + formatTime(session.getDatabaseFilters().getTimeFilter().getStart()));
        }

        if (session.getDatabaseFilters().getRadiusFilter() != null) {
            player.sendMessage("§7Radio: §f" + session.getDatabaseFilters().getRadiusFilter().getRadius());
        }

        player.sendMessage("§7§m----------------------------------");
        player.sendMessage("§eComandos:");
        player.sendMessage("§f/sp irs toggle §7- Activar/desactivar modo");
        player.sendMessage("§f/sp irs stop §7- Detener sesión");
    }

    private String formatTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long hours = diff / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);

        if (hours > 0) {
            return hours + "h " + minutes + "m atrás";
        } else {
            return minutes + "m atrás";
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length == 1) {
            return Arrays.asList("toggle", "stop", "status");
        }
        return Arrays.asList();
    }

}

