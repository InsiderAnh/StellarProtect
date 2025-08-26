package io.github.insideranh.stellarprotect.commands.arguments.basic;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.StellarArgument;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugArgument extends StellarArgument {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length < 1) {
            plugin.getLangManager().sendMessage(sender, "messages.noDebug");
            return;
        }
        String debugType = arguments[0].toLowerCase();
        switch (debugType) {
            case "save": {
                boolean newState = !plugin.getConfigManager().isDebugSave();
                plugin.getConfig().set("debugs.save", newState);
                plugin.saveConfig();
                plugin.getConfigManager().setDebugSave(newState);

                plugin.getLangManager().sendMessage(sender, "messages.debugSave", text -> text.replace("<state>", newState ? "on" : "off"));
                break;
            }
            case "log": {
                boolean newState = !plugin.getConfigManager().isDebugLog();
                plugin.getConfig().set("debugs.log", newState);
                plugin.saveConfig();
                plugin.getConfigManager().setDebugLog(newState);

                plugin.getLangManager().sendMessage(sender, "messages.debugLog", text -> text.replace("<state>", newState ? "on" : "off"));
                break;
            }
            case "extras": {
                boolean newState = !plugin.getConfigManager().isDebugExtras();
                plugin.getConfig().set("debugs.extras", newState);
                plugin.saveConfig();
                plugin.getConfigManager().setDebugExtras(newState);

                plugin.getLangManager().sendMessage(sender, "messages.debugExtras", text -> text.replace("<state>", newState ? "on" : "off"));
                break;
            }
            default:
                sender.sendMessage("Invalid debug type.");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>(Arrays.asList("save", "log", "extras"));
    }

}
