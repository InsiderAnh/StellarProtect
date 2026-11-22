package io.github.insideranh.stellarprotect.commands.arguments.lookups;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.LocationArg;
import io.github.insideranh.stellarprotect.arguments.PageArg;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.InspectSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NextInspectArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            plugin.getLangManager().sendMessage(sender, "messages.onlyPlayer");
            return;
        }
        LocationArg locationArg = ArgumentsParser.parseLocation(arguments);
        PageArg pageArg = ArgumentsParser.parsePage(arguments);

        if (locationArg == null) {
            plugin.getLangManager().sendMessage(sender, "messages.noLocation");
            return;
        }

        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        World world = Bukkit.getWorld(locationArg.getWorld());
        if (world == null) {
            plugin.getLangManager().sendMessage(sender, "messages.noLocation");
            return;
        }
        Location blockLocation = new Location(world, locationArg.getX(), locationArg.getY(), locationArg.getZ());

        boolean hasChestArg = false;
        for (String arg : arguments) {
            String lower = arg.toLowerCase();
            if (lower.equals("c:1") || lower.equals("c:true") || lower.equals("type:chest") || lower.equals("chest:true") || lower.equals("chest:1")) {
                hasChestArg = true;
                break;
            }
        }

        InspectSession inspectSession = playerProtect.getInspectSession();
        boolean isChestInspection;
        if (hasChestArg) {
            isChestInspection = true;
        } else if (inspectSession != null) {
            isChestInspection = inspectSession.isInspectChest();
        } else {
            isChestInspection = false;
        }

        if (playerProtect.getNextInspect() > System.currentTimeMillis())
            return;
        playerProtect.setNextInspect(System.currentTimeMillis() + 500L);
        playerProtect.getPosibleLogs().clear();

        playerProtect.setInspectSession(new InspectSession(blockLocation, pageArg.getSkip(), pageArg.getPerPage(), isChestInspection));

        if (isChestInspection) {
            plugin.getInspectHandler().handleChestInspection(player, blockLocation, pageArg.getPage(), pageArg.getSkip(), pageArg.getPerPage());
        } else {
            plugin.getInspectHandler().handleBlockInspection(player, blockLocation, pageArg.getPage(), pageArg.getSkip(), pageArg.getPerPage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>();
    }

}
