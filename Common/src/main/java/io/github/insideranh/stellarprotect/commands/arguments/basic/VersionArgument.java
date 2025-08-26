package io.github.insideranh.stellarprotect.commands.arguments.basic;

import io.github.insideranh.stellarprotect.commands.StellarArgument;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VersionArgument extends StellarArgument {

    @Override
    public void onCommand(@NotNull CommandSender sender, String[] arguments) {
        sender.sendMessage("§7╭───────────────────────╮");
        sender.sendMessage("§7          §6✦ §dStellarProtect §6✦");
        sender.sendMessage("§7       §fAdvanced Protection System");
        sender.sendMessage("§7╰───────────────────────╯");
        sender.sendMessage("§7§6");
        sender.sendMessage("§7▸ §fVersion: §a0.0.1");
        sender.sendMessage("§7▸ §fDeveloper: §dInsiderAnh");
        sender.sendMessage("§a§4");
        sender.sendMessage("§7▸ §fSpigot:§7 https://shorturl.at/3fexE");
        sender.sendMessage("§7▸ §fModrinth:§7 https://shorturl.at/Pee6K");
        sender.sendMessage("§7▸ §fHangar:§7 https://shorturl.at/4fndy");
        sender.sendMessage("§7▸ §fBuildByBit:§7 https://shorturl.at/yHYvu");
        sender.sendMessage("§8§9");
        sender.sendMessage("§7▸ §fWiki:§d https://shorturl.at/qCuWS");
        sender.sendMessage("§7▸ §fPatreon:§d https://www.patreon.com/c/insideranh/membership");
        sender.sendMessage("§5§6");
        sender.sendMessage("§7  §fIf you like this plugin, please consider supporting me!");
        sender.sendMessage("§7       §fThank you for using §6StellarProtect§f!");
        sender.sendMessage("§7           §fMade with §c❤ §ffor the community");
        sender.sendMessage("§5§a");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>();
    }

}
