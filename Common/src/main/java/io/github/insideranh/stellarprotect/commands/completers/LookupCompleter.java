package io.github.insideranh.stellarprotect.commands.completers;

import com.google.common.base.Joiner;
import io.github.insideranh.stellarprotect.commands.StellarCompleter;
import io.github.insideranh.stellarprotect.enums.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LookupCompleter extends StellarCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] arguments) {
        if (arguments.length == 0) {
            return getDefaultSuggestions();
        }

        ArgumentParser parser = new ArgumentParser();
        parser.parseExistingArguments(arguments);

        String currentArg = arguments[arguments.length - 1];

        if (!currentArg.contains(":")) {
            return getAvailableArguments(parser);
        }

        String[] parts = currentArg.split(":", 2);
        if (parts.length != 2) {
            return getAvailableArguments(parser);
        }

        String prefix = parts[0];
        String value = parts[1];
        ArgumentType currentType = ArgumentType.fromPrefix(prefix);

        if (currentType == null) {
            return getAvailableArguments(parser);
        }

        return getSuggestionsForArgument(currentType, prefix, value, parser);
    }

    private List<String> getDefaultSuggestions() {
        return Arrays.asList("t:1h", "r:10", "p:1-10", "a:block_break", "u:player", "i:grass", "e:stone", "mi:[display:&aAmazing sword]");
    }

    private List<String> getAvailableArguments(ArgumentParser parser) {
        List<String> suggestions = new ArrayList<>();

        for (ArgumentType type : ArgumentType.values()) {
            if (type == ArgumentType.RADIUS || type == ArgumentType.TIME || type == ArgumentType.PAGE) {
                if (parser.isArgumentUsed(type)) {
                    continue;
                }
            }

            suggestions.add(type.getShortPrefix() + ":");
        }

        return suggestions;
    }

    private List<String> getSuggestionsForArgument(ArgumentType type, String usedPrefix, String value, ArgumentParser parser) {
        switch (type) {
            case MATERIAL_INCLUDES:
                if (value.isEmpty()) {
                    return Arrays.asList("mi:[display:&aAmazing sword]", "mi:[lore:&7The best sword]");
                }
                return Collections.emptyList();
            case MATERIAL_EXCLUDES:
                if (value.isEmpty()) {
                    return Arrays.asList("me:[display:&aAmazing sword]", "me:[lore:&7The best sword]");
                }
                return Collections.emptyList();
            case ACTION:
                return handleActionSuggestions(usedPrefix, value);

            case TIME:
                if (parser.isArgumentUsed(type) && !value.isEmpty()) {
                    return Collections.emptyList();
                }
                return Arrays.asList(
                    usedPrefix + ":1h",
                    usedPrefix + ":1d",
                    usedPrefix + ":1w",
                    usedPrefix + ":1mo"
                );

            case RADIUS:
                if (parser.isArgumentUsed(type) && !value.isEmpty()) {
                    return Collections.emptyList();
                }
                return Arrays.asList(
                    usedPrefix + ":10",
                    usedPrefix + ":20",
                    usedPrefix + ":30",
                    usedPrefix + ":40",
                    usedPrefix + ":50"
                );

            case PAGE:
                if (parser.isArgumentUsed(type) && !value.isEmpty()) {
                    return Collections.emptyList();
                }
                return Arrays.asList(
                    usedPrefix + ":1-10",
                    usedPrefix + ":2-10",
                    usedPrefix + ":1-20"
                );
            case USERS:
                return handleUserSuggestions(usedPrefix, value);

            default:
                return Collections.emptyList();
        }
    }

    private List<String> handleActionSuggestions(String usedPrefix, String value) {
        String[] existingActions = value.split(",");

        if (value.isEmpty() || existingActions.length == 0) {
            return ActionType.getAllNames(null);
        }

        List<String> suggestions = ActionType.getAllNamesNoPrefix(null);
        List<String> usedActions = new LinkedList<>();
        List<String> completions = new LinkedList<>();

        String lastAction = existingActions[existingActions.length - 1].trim();

        for (String part : existingActions) {
            String partTrimmed = part.trim();

            if (partTrimmed.equals(lastAction)) continue;
            if (partTrimmed.isEmpty()) continue;

            suggestions.remove(partTrimmed);
            usedActions.add(partTrimmed);
        }

        for (String action : suggestions) {
            /*
            if (!lastAction.isEmpty() && !action.startsWith(lastAction)) {
                Debugger.debugExtras("Returned: " + action + " | " + lastAction + " | " + action.startsWith(lastAction));
                continue;
            }
             */

            if (usedActions.isEmpty()) {
                completions.add(usedPrefix + ":" + action);
            } else {
                completions.add(usedPrefix + ":" + Joiner.on(",").join(usedActions) + "," + action);
            }
        }

        if (completions.isEmpty()) {
            String prefix;
            if (usedActions.isEmpty()) {
                prefix = usedPrefix + ":" + lastAction;
            } else {
                prefix = usedPrefix + ":" + Joiner.on(",").join(usedActions) + "," + lastAction;
            }
            return Collections.singletonList(prefix);
        }

        return completions;
    }

    private List<String> handleUserSuggestions(String usedPrefix, String value) {
        String[] existingUsers = value.split(",");

        if (value.isEmpty() || existingUsers.length == 0) {
            List<String> suggestions = new ArrayList<>();
            AtomicInteger count = new AtomicInteger(0);

            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(usedPrefix + ":" + player.getName());
                count.getAndIncrement();
                if (count.get() >= 10) {
                    break;
                }
            }
            return suggestions;
        }

        List<String> usedUsers = new LinkedList<>();
        List<String> completions = new LinkedList<>();
        String lastUser = existingUsers[existingUsers.length - 1];

        for (String part : value.split(",")) {
            String partTrimmed = part.trim();
            if (partTrimmed.equals(lastUser)) continue;
            usedUsers.add(partTrimmed);
        }

        String prefix = usedPrefix + ":" + String.join(",", usedUsers);
        AtomicInteger count = new AtomicInteger(0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();

            if (!playerName.toLowerCase().contains(lastUser.toLowerCase())) continue;
            if (usedUsers.contains(playerName)) continue;

            if (usedUsers.isEmpty()) {
                completions.add(usedPrefix + ":" + playerName);
            } else {
                completions.add(prefix + "," + playerName);
            }

            count.getAndIncrement();
            if (count.get() >= 10) {
                break;
            }
        }

        if (completions.isEmpty()) {
            if (usedUsers.isEmpty()) {
                return Collections.singletonList(usedPrefix + ":" + lastUser);
            } else {
                return Collections.singletonList(prefix + "," + lastUser);
            }
        }

        return completions;
    }

    public enum ArgumentType {
        ACTION("a", "action"),
        TIME("t", "time"),
        RADIUS("r", "radius"),
        PAGE("p", "page"),
        USERS("u", "users"),
        INCLUDES("i", "include"),
        EXCLUDES("e", "exclude"),
        MATERIAL_INCLUDES("mi", "material_includes"),
        MATERIAL_EXCLUDES("me", "material_excludes");

        private final String shortPrefix;
        private final String longPrefix;

        ArgumentType(String shortPrefix, String longPrefix) {
            this.shortPrefix = shortPrefix;
            this.longPrefix = longPrefix;
        }

        public static ArgumentType fromPrefix(String prefix) {
            for (ArgumentType type : values()) {
                if (type.matchesPrefix(prefix)) {
                    return type;
                }
            }
            return null;
        }

        public String getShortPrefix() {
            return shortPrefix;
        }

        public String getLongPrefix() {
            return longPrefix;
        }

        public boolean matchesPrefix(String prefix) {
            return prefix.equals(shortPrefix) || prefix.equals(longPrefix);
        }
    }

    public static class ArgumentParser {
        private final Set<ArgumentType> usedArguments = new HashSet<>();

        public void parseExistingArguments(String[] arguments) {
            usedArguments.clear();

            for (String arg : arguments) {
                if (arg.contains(":")) {
                    String prefix = arg.split(":", 2)[0];
                    ArgumentType type = ArgumentType.fromPrefix(prefix);
                    if (type != null) {
                        usedArguments.add(type);
                    }
                }
            }
        }

        public boolean isArgumentUsed(ArgumentType type) {
            return usedArguments.contains(type);
        }

        public Set<ArgumentType> getUsedArguments() {
            return new HashSet<>(usedArguments);
        }
    }

}