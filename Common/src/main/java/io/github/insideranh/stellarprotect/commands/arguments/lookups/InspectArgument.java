package io.github.insideranh.stellarprotect.commands.arguments.lookups;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InspectArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().sendMessage(sender, "messages.onlyPlayer");
            return;
        }
        Player player = arguments.length > 1 && sender.hasPermission("stellarprotect.inspect.others") ? plugin.getServer().getPlayer(arguments[1]) : (Player) sender;
        if (player == null) {
            plugin.getLangManager().sendMessage(sender, "messages.offlinePlayer");
            return;
        }
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) {
            plugin.getLangManager().sendMessage(sender, "messages.noPlayer");
            return;
        }
        playerProtect.setInspect(!playerProtect.isInspect());
        playerProtect.getPosibleLogs().clear();
        playerProtect.setInspectSession(null);
        playerProtect.setLookupSession(null);

        plugin.getLangManager().sendMessage(sender, "messages.inspect." + (playerProtect.isInspect() ? "enable" : "disable"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>();
    }

}