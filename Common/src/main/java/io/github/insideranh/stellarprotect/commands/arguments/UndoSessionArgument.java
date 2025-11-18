package io.github.insideranh.stellarprotect.commands.arguments;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.data.UndoSession;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class UndoSessionArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null || playerProtect.getUndoSession() == null) {
            plugin.getLangManager().sendMessage(player, "messages.undo.noActiveUndoSession");
            return;
        }

        UndoSession session = playerProtect.getUndoSession();

        if (arguments.length == 0) {
            return;
        }

        String action = arguments[0].toLowerCase();

        switch (action) {
            case "undo":
                if (arguments.length > 1) {
                    plugin.getUndoSessionManager().undoIndividualLog(session, Integer.parseInt(arguments[1]));
                }
                break;
            case "redo":
                if (arguments.length > 1) {
                    plugin.getUndoSessionManager().redoIndividualLog(session, Integer.parseInt(arguments[1]));
                }
                break;
            case "undoall":
                plugin.getUndoSessionManager().undoAllLogs(session);
                break;
            case "next":
                plugin.getUndoSessionManager().showUndoSession(session);
                break;
            case "exit":
                plugin.getUndoSessionManager().exitSession(session);
                break;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return Arrays.asList("undo", "redo", "undoall", "next", "exit");
    }

}

