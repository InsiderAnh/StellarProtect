package io.github.insideranh.stellarprotect.commands;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class StellarArgument {

    public abstract void onCommand(@NotNull CommandSender sender, String[] arguments);

    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        return new ArrayList<>();
    }

}