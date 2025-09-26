package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.data.RestoreSession;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class RestoreSessionArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null || playerProtect.getRestoreSession() == null) {
            plugin.getLangManager().sendMessage(player, "messages.noActiveRestoreSession");
            return;
        }

        RestoreSession session = playerProtect.getRestoreSession();

        if (arguments.length == 0) {
            return;
        }

        String action = arguments[0].toLowerCase();

        switch (action) {
            case "restore":
                if (arguments.length > 1) {
                    plugin.getRestoreSessionManager().restoreIndividualLog(session, Integer.parseInt(arguments[1]));
                }
                break;
            case "undo":
                if (arguments.length > 1) {
                    plugin.getRestoreSessionManager().undoIndividualRestore(session, Integer.parseInt(arguments[1]));
                }
                break;
            case "restoreall":
                plugin.getRestoreSessionManager().restoreAllVisible(session);
                break;
            case "next":
                plugin.getRestoreSessionManager().nextPage(session);
                break;
            case "exit":
                plugin.getRestoreSessionManager().exitSession(session);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return Arrays.asList("restore", "undo", "restoreall", "next", "exit");
    }

}