package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;
        if (arguments.length < 4) {
            return;
        }
        World world = Bukkit.getWorld(arguments[0]);
        if (world == null) {
            return;
        }
        double x = Double.parseDouble(arguments[1]);
        double y = Double.parseDouble(arguments[2]);
        double z = Double.parseDouble(arguments[3]);
        plugin.getProtectNMS().teleport(player, new Location(world, x, y, z));
    }

}