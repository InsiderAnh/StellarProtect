package io.github.insideranh.stellarprotect.commands.arguments.views;

import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.menus.ViewArmorStandItemMenu;
import io.github.insideranh.stellarprotect.menus.ViewInventoryMenu;
import io.github.insideranh.stellarprotect.menus.ViewItemMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ViewArgument extends StellarArgument {

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            return;
        }
        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        String view = arguments[0];
        switch (view) {
            case "inventory": {
                Object object = playerProtect.getPosibleLogs().get(Integer.parseInt(arguments[1]));
                if (object != null) {
                    new ViewInventoryMenu(player, object).open();
                }
                break;
            }
            case "item": {
                Object object = playerProtect.getPosibleLogs().get(Integer.parseInt(arguments[1]));
                if (object != null) {
                    new ViewItemMenu(player, object).open();
                }
                break;
            }
            case "stand": {
                Object object = playerProtect.getPosibleLogs().get(Integer.parseInt(arguments[1]));
                if (object != null) {
                    playerProtect.getPosibleLogs().put(Integer.parseInt(arguments[1]), object);
                    new ViewArmorStandItemMenu(player, object).open();
                }
                break;
            }
            default:
                break;
        }
    }

}