package io.github.insideranh.stellarprotect.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class StellarCompleter {

    public abstract List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments);

}