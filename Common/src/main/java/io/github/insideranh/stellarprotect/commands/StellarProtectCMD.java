package io.github.insideranh.stellarprotect.commands;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.commands.arguments.PurgeArgument;
import io.github.insideranh.stellarprotect.commands.arguments.RestoreArgument;
import io.github.insideranh.stellarprotect.commands.arguments.TeleportArgument;
import io.github.insideranh.stellarprotect.commands.arguments.basic.DebugArgument;
import io.github.insideranh.stellarprotect.commands.arguments.basic.MemoryArgument;
import io.github.insideranh.stellarprotect.commands.arguments.basic.VersionArgument;
import io.github.insideranh.stellarprotect.commands.arguments.lookups.InspectArgument;
import io.github.insideranh.stellarprotect.commands.arguments.lookups.LookupArgument;
import io.github.insideranh.stellarprotect.commands.arguments.lookups.NextInspectArgument;
import io.github.insideranh.stellarprotect.commands.arguments.lookups.NextLookupArgument;
import io.github.insideranh.stellarprotect.commands.arguments.views.ViewArgument;
import io.github.insideranh.stellarprotect.commands.completers.LookupCompleter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StellarProtectCMD implements TabExecutor {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private final HashMap<String, StellarArgument> arguments = new HashMap<>();
    private final HashMap<String, StellarCompleter> completes = new HashMap<>();

    public StellarProtectCMD() {
        arguments.put("lookup", new LookupArgument());
        arguments.put("nextlookup", new NextLookupArgument());
        arguments.put("inspect", new InspectArgument());
        arguments.put("purge", new PurgeArgument());
        arguments.put("nextinspect", new NextInspectArgument());
        arguments.put("debug", new DebugArgument());
        arguments.put("memory", new MemoryArgument());
        arguments.put("version", new VersionArgument());
        arguments.put("restore", new RestoreArgument());
        arguments.put("teleport", new TeleportArgument());
        arguments.put("view", new ViewArgument());

        completes.put("lookup", new LookupCompleter());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            if (hasBlockedPermission(sender, "default")) {
                return false;
            }
            sendHelp(sender);
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                if (hasBlockedPermission(sender, "admin")) {
                    return false;
                }
                plugin.reload();
                plugin.getLangManager().sendMessage(sender, "messages.reloaded");
                break;
            case "i":
            case "inspect": {
                if (hasBlockedPermission(sender, "inspect")) {
                    return false;
                }
                arguments.get("inspect").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "ni":
            case "nextinspect": {
                if (hasBlockedPermission(sender, "inspect")) {
                    return false;
                }
                arguments.get("nextinspect").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "t":
            case "teleport": {
                if (hasBlockedPermission(sender, "lookup") && hasBlockedPermission(sender, "teleport")) {
                    return false;
                }
                arguments.get("teleport").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "s":
            case "l":
            case "search":
            case "lookup": {
                if (hasBlockedPermission(sender, "lookup")) {
                    return false;
                }
                arguments.get("lookup").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "nl":
            case "nextlookup": {
                if (hasBlockedPermission(sender, "lookup")) {
                    return false;
                }
                arguments.get("nextlookup").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "view":
                if (hasBlockedPermission(sender, "view")) {
                    return false;
                }
                arguments.get("view").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "purge":
                if (hasBlockedPermission(sender, "purge")) {
                    return false;
                }
                arguments.get("purge").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "debug":
                if (hasBlockedPermission(sender, "admin")) {
                    return false;
                }
                arguments.get("debug").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "v":
            case "version":
                if (hasBlockedPermission(sender, "default")) {
                    return false;
                }
                arguments.get("version").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "mem":
            case "memory":
                if (hasBlockedPermission(sender, "memory")) {
                    return false;
                }
                arguments.get("memory").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "restore":
            case "rollback":
                if (hasBlockedPermission(sender, "rollback")) {
                    return false;
                }
                arguments.get("restore").onCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "help":
            default:
                if (hasBlockedPermission(sender, "default")) {
                    return false;
                }
                sendHelp(sender);
                break;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length <= 1) {
            String arg = args.length == 1 ? args[0].toLowerCase() : "";
            return Stream.of("reload", "version", "help", "lookup", "inspect", "purge", "memory", "debug", "restore").filter(s -> arg.isEmpty() || s.startsWith(arg)).collect(Collectors.toList());
        }
        String arg = args[0].toLowerCase();
        switch (arg) {
            case "inspect":
            case "i": {
                return arguments.get("inspect").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "search":
            case "lookup":
            case "s":
            case "l": {
                return completes.get("lookup").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "purge": {
                return arguments.get("purge").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "debug": {
                return arguments.get("debug").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "mem":
            case "memory": {
                return arguments.get("memory").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "restore":
            case "rollback": {
                return arguments.get("restore").onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            default: {
                return Stream.of("reload", "version", "help", "lookup", "inspect", "purge", "memory", "debug", "restore").filter(s -> s.startsWith(arg)).collect(Collectors.toList());
            }
        }
    }

    void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "+---------------------------------------+");
        sender.sendMessage(ChatColor.DARK_GRAY + "[!] " + ChatColor.RED + "StellarProtect " + ChatColor.DARK_GRAY + "[!]");
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "+---------------------------------------+");
        sender.sendMessage(ChatColor.YELLOW + "/spt reload " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Reload the plugin.");
        sender.sendMessage(ChatColor.YELLOW + "/spt help " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Show this help.");
        sender.sendMessage(ChatColor.YELLOW + "/spt version " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Show the plugin version.");
        sender.sendMessage(ChatColor.YELLOW + "/spt inspect " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Enable/disable inspection mode.");
        sender.sendMessage(ChatColor.YELLOW + "/spt lookup p:<page> t:<time> r:<radius> " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Search for logs in a radius.");
        sender.sendMessage(ChatColor.YELLOW + "/spt restore p:<page> t:<time> r:<radius> " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Restore logs in a radius.");
        sender.sendMessage(ChatColor.YELLOW + "/spt purge p:<page> t:<time> r:<radius> " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Purge logs in a radius.");
        sender.sendMessage(ChatColor.YELLOW + "/spt debug save|log|extras " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Enable/disable debug mode.");
        sender.sendMessage(ChatColor.YELLOW + "/spt memory " + ChatColor.GRAY + "- " + ChatColor.WHITE + "Show memory analysis.");
    }

    boolean hasBlockedPermission(CommandSender sender, String permission) {
        if (sender.hasPermission("stellarprotect." + permission) || sender.hasPermission("stellarprotect.admin") || sender.isOp()) {
            return false;
        }
        plugin.getLangManager().sendMessage(sender, "messages.noPermission");
        return true;
    }

}
