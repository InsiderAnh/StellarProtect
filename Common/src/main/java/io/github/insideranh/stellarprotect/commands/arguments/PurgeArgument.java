package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.arguments.ArgumentsParser;
import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import io.github.insideranh.stellarprotect.arguments.RadiusArg;
import io.github.insideranh.stellarprotect.arguments.TimeArg;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PurgeArgument extends StellarArgument {

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

        TimeArg timeArg = ArgumentsParser.parseTime(arguments);
        RadiusArg radiusArg = ArgumentsParser.parseRadiusOrNull(player, arguments, player.getLocation());
        List<ActionType> actionTypesArg = ArgumentsParser.parseActionTypes(arguments);
        plugin.getLangManager().sendMessage(sender, "messages.purging");

        ArgumentsParser.parseUsers(arguments).thenAccept(usersArg -> {
            DatabaseFilters databaseFilters = new DatabaseFilters();
            databaseFilters.setTimeFilter(timeArg);
            databaseFilters.setRadiusFilter(radiusArg);
            databaseFilters.setUserFilters(usersArg);
            databaseFilters.setActionTypesFilter(actionTypesArg.stream().map(ActionType::getId).collect(Collectors.toCollection(ArrayList::new)));

            plugin.getProtectDatabase().purgeLogs(databaseFilters, inMs -> sender.sendMessage(plugin.getLangManager().get("messages.purgedFinish").replace("<time>", String.valueOf(inMs))));
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length >= 1) {
            String currentArg = arguments[arguments.length - 1];

            if (currentArg.startsWith("a:") || currentArg.startsWith("action:")) {
                return ActionType.getAllNames(currentArg.replaceFirst("a:", "").replaceFirst("action:", "").toLowerCase());
            }

            if (currentArg.startsWith("t:") || currentArg.startsWith("time:")) {
                return Arrays.asList("t:1h", "t:1d", "t:1w", "t:1mo");
            }

            if (currentArg.startsWith("r:") || currentArg.startsWith("radius:")) {
                return Arrays.asList("r:10", "r:20", "r:30", "r:40", "r:50");
            }

            if (currentArg.startsWith("u:") || currentArg.startsWith("users:")) {
                String name = currentArg.replaceFirst("^(u:|users:)", "");
                List<String> names = new ArrayList<>();
                AtomicInteger max = new AtomicInteger(0);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().contains(name.toLowerCase())) {
                        names.add("u:" + player.getName());
                        max.getAndIncrement();
                        if (max.get() >= 10) {
                            break;
                        }
                    }
                }

                return names;
            }
        }

        return Arrays.asList("t:1h", "r:10", "p:1-10", "a:block_break");
    }

}