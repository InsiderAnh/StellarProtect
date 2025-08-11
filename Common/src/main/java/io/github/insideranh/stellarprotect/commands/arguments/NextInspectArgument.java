package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.LocationArg;
import io.github.insideranh.stellarprotect.arguments.PageArg;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.InspectSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import org.bukkit.Location;
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

        InspectSession inspectSession = playerProtect.getInspectSession();
        if (inspectSession == null) {
            plugin.getLangManager().sendMessage(sender, "messages.noInspectSession");
            return;
        }

        if (playerProtect.getNextInspect() > System.currentTimeMillis())
            return;
        playerProtect.setNextInspect(System.currentTimeMillis() + 500L);

        Location blockLocation = inspectSession.getLocation().getBlock().getLocation();
        playerProtect.setInspectSession(new InspectSession(blockLocation, pageArg.getSkip(), pageArg.getPerPage(), WorldUtils.isValidChestBlock(inspectSession.getLocation().getBlock().getType())));

        if (WorldUtils.isValidChestBlock(inspectSession.getLocation().getBlock().getType())) {
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
