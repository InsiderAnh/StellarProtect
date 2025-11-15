package io.github.insideranh.stellarprotect.arguments;

import javax.annotation.Nullable;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.TimesType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentsParser {

    public static final String MATERIAL_TYPE = "material_type";
    public static final String DISPLAY = "display";
    public static final String LORE = "lore";
    private static final Pattern TIME_PATTERN = Pattern.compile("([\\d.]+)(mo|[ywdhms])");
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("(?:mi:|material_includes:)(\\[([^:]+):([^\\]]+)\\]|([^\\s\\[]+))");
    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("(?:me:|material_excludes:)(\\[([^:]+):([^\\]]+)\\]|([^\\s\\[]+))");

    static int parseDuration(String input) {
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase(Locale.ROOT));
        int totalSeconds = 0;

        while (matcher.find()) {
            double value = parseSafeDouble(matcher.group(1));
            String symbol = matcher.group(2);

            for (TimesType unit : TimesType.values()) {
                if (unit.getSymbol().equals(symbol)) {
                    totalSeconds += (int) (value * unit.getSeconds());
                    break;
                }
            }
        }
        return totalSeconds;
    }

    static double parseSafeDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    static int parseSafeInteger(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static List<ActionType> parseActionTypes(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "");

        List<ActionType> actionTypes = new ArrayList<>();
        for (String part : joined.split("\\s+")) {
            if (part.startsWith("a:") || part.startsWith("action:")) {
                String actionType = part.replaceFirst("^(a:|action:)", "");
                for (String action : actionType.split(",")) {
                    actionTypes.add(ActionType.getByName(action));
                }
            }
        }
        return actionTypes;
    }

    public static List<String> parseIncludesMaterials(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        List<String> actionTypes = new ArrayList<>();
        for (String part : joined.split("\\s+")) {
            if (part.startsWith("i:") || part.startsWith("include:")) {
                String actionType = part.replaceFirst("^(i:|include:)", "");
                actionTypes.addAll(Arrays.asList(actionType.split(",")));
            }
        }
        return actionTypes;
    }

    public static List<String> parseExcludesMaterials(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        List<String> actionTypes = new ArrayList<>();
        for (String part : joined.split("\\s+")) {
            if (part.startsWith("e:") || part.startsWith("exclude:")) {
                String actionType = part.replaceFirst("^(e:|exclude:)", "");
                actionTypes.addAll(Arrays.asList(actionType.split(",")));
            }
        }
        return actionTypes;
    }

    public static @Nullable LocationArg parseLocation(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("l:") || part.startsWith("location:")) {
                String locationSegment = part.replaceFirst("^(l:|location:)", "");
                String[] data = locationSegment.split(";");

                if (data.length == 4) {
                    return new LocationArg(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]));
                }
            }
        }

        return null;
    }

    public static @NonNull TimeArg parseTime(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("t:") || part.startsWith("time:")) {
                String timeSegment = part.replaceFirst("^(t:|time:)", "");
                timeSegment = timeSegment.replace(",", "");

                String[] rangeParts = timeSegment.split("-");

                if (rangeParts.length == 0) {
                    return new TimeArg("", "", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), System.currentTimeMillis());
                }

                String startString = rangeParts[0];
                String endString = rangeParts.length > 1 ? rangeParts[1] : "";

                long startDurationMs = parseEnhancedDuration(rangeParts[0]) * 1000L;
                long endDurationMs = rangeParts.length > 1 ? parseEnhancedDuration(rangeParts[1]) * 1000L : 0;

                long timeStart = System.currentTimeMillis() - startDurationMs;
                long timeEnd = endDurationMs > 0 ? System.currentTimeMillis() - endDurationMs : System.currentTimeMillis();

                if (timeStart > timeEnd) {
                    long temp = timeStart;
                    timeStart = timeEnd;
                    timeEnd = temp;
                }

                return new TimeArg(startString, endString, timeStart, timeEnd);
            }
        }
        return new TimeArg("", "", 0, System.currentTimeMillis());
    }

    private static long parseEnhancedDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }

        duration = duration.toLowerCase().trim();
        long totalSeconds = 0;

        Pattern pattern = Pattern.compile("(\\d*\\.?\\d+)(mo|[ywdhms])");
        Matcher matcher = pattern.matcher(duration);

        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "y":
                    totalSeconds += (long) (value * 365 * 24 * 3600);
                    break;
                case "mo":
                    totalSeconds += (long) (value * 30 * 24 * 3600);
                    break;
                case "w":
                    totalSeconds += (long) (value * 7 * 24 * 3600);
                    break;
                case "d":
                    totalSeconds += (long) (value * 24 * 3600);
                    break;
                case "h":
                    totalSeconds += (long) (value * 3600);
                    break;
                case "m":
                    totalSeconds += (long) (value * 60);
                    break;
                case "s":
                    totalSeconds += (long) value;
                    break;
            }
        }

        if (totalSeconds == 0) {
            try {
                totalSeconds = (long) Double.parseDouble(duration);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return totalSeconds;
    }

    public static @NonNull PageArg parsePage(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("p:") || part.startsWith("page:")) {
                String timeSegment = part.replaceFirst("^(p:|page:)", "");
                String[] rangeParts = timeSegment.split("-");
                if (rangeParts.length == 0) {
                    rangeParts = new String[]{"1", "10"};
                }
                if (rangeParts.length == 1) {
                    rangeParts = new String[]{rangeParts[0], "10"};
                }

                int page = parseSafeInteger(rangeParts[0], 1);
                int perPage = parseSafeInteger(rangeParts[1], 10);

                return new PageArg(page, perPage);
            }
        }
        return new PageArg(1, 10);
    }

    public static @NonNull CompletableFuture<UsersArg> parseUsers(String[] arguments) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("u:") || part.startsWith("users:")) {
                String usersSegment = part.replaceFirst("^(u:|users:)", "");
                String[] userNames = usersSegment.split(",");

                return CompletableFuture.supplyAsync(() -> {
                    Set<Long> userIds = new HashSet<>();
                    List<String> notFoundNames = new ArrayList<>();
                    for (String userName : userNames) {
                        userName = userName.trim();
                        if (!userName.isEmpty()) {
                            long userId = PlayerUtils.getPlayerOrEntityId(userName);
                            if (userId != -2) {
                                userIds.add(userId);
                            } else {
                                notFoundNames.add(userName);
                            }
                        }
                    }
                    List<Long> dbUserIds = StellarProtect.getInstance().getProtectDatabase().getIdsByNames(notFoundNames);
                    userIds.addAll(dbUserIds);

                    return new UsersArg(userIds);
                });
            }
        }

        return CompletableFuture.completedFuture(new UsersArg());
    }


    public static @Nullable RadiusArg parseRadiusOrNull(Player player, String[] arguments, @NonNull Location location) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("r:") || part.startsWith("radius:")) {
                String radiusSegment = part.replaceFirst("^(r:|radius:)", "");
                if (radiusSegment.isEmpty()) continue;

                int worldId;
                String[] radiusParts;

                if (radiusSegment.startsWith("#")) {
                    if (radiusSegment.startsWith("#we") && StellarProtect.getInstance().getWorldEditHook() != null) {
                        return StellarProtect.getInstance().getWorldEditHook().getRadiusArgWorldEdit(player);
                    }

                    if (radiusSegment.equals("#global")) {
                        return new RadiusArg(-1, 100000,
                            location.getBlockX() - 100000, location.getBlockX() + 100000,
                            location.getBlockY() - 100000, location.getBlockY() + 100000,
                            location.getBlockZ() - 100000, location.getBlockZ() + 100000);
                    }

                    String[] worldParts = radiusSegment.substring(1).split(",");
                    String worldName = worldParts[0];
                    worldId = WorldUtils.searchWorldId(worldName);

                    if (worldParts.length > 1) {
                        radiusParts = new String[worldParts.length - 1];
                        System.arraycopy(worldParts, 1, radiusParts, 0, worldParts.length - 1);
                    } else {
                        return new RadiusArg(worldId, 10000,
                            location.getBlockX() - 10000, location.getBlockX() + 10000,
                            location.getBlockY() - 10000, location.getBlockY() + 10000,
                            location.getBlockZ() - 10000, location.getBlockZ() + 10000);
                    }
                } else {
                    worldId = WorldUtils.searchWorldId(location.getWorld().getName());
                    radiusParts = radiusSegment.split(",");
                }

                try {
                    double radiusX, radiusY, radiusZ;

                    radiusX = radiusParts.length > 0 ? Double.parseDouble(radiusParts[0].trim()) : 10;

                    if (radiusParts.length >= 3) {
                        radiusY = Double.parseDouble(radiusParts[1].trim());
                        radiusZ = Double.parseDouble(radiusParts[2].trim());
                    } else if (radiusParts.length == 2) {
                        radiusY = Double.parseDouble(radiusParts[1].trim());
                        radiusZ = radiusX;
                    } else {
                        radiusY = radiusX;
                        radiusZ = radiusX;
                    }

                    double maxRadius = Math.max(Math.max(radiusX, radiusY), radiusZ);

                    return new RadiusArg(worldId, maxRadius,
                        location.getBlockX() - radiusX,
                        location.getBlockX() + radiusX,
                        location.getBlockY() - radiusY,
                        location.getBlockY() + radiusY,
                        location.getBlockZ() - radiusZ,
                        location.getBlockZ() + radiusZ);

                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public static Map<String, List<String>> parseIncludeMaterials(String[] arguments) {
        String joined = String.join(" ", arguments).replace("\\", "").replace("'", "");
        return parseFilterArguments(joined, INCLUDE_PATTERN);
    }

    public static Map<String, List<String>> parseExcludeMaterials(String[] arguments) {
        String joined = String.join(" ", arguments).replace("\\", "").replace("'", "");
        return parseFilterArguments(joined, EXCLUDE_PATTERN);
    }

    private static Map<String, List<String>> parseFilterArguments(String joined, Pattern pattern) {
        Map<String, List<String>> filterMap = new HashMap<>();
        Matcher matcher = pattern.matcher(joined);

        while (matcher.find()) {
            if (matcher.group(2) != null && matcher.group(3) != null) {
                String filterType = matcher.group(2).trim();
                String filterValue = matcher.group(3).trim();

                String mappedFilterType = mapFilterType(filterType);

                filterMap.computeIfAbsent(mappedFilterType, k -> new ArrayList<>()).add(filterValue);
            } else if (matcher.group(4) != null) {
                String values = matcher.group(4);
                String[] valueArray = values.split(",");

                List<String> materialList = filterMap.computeIfAbsent(MATERIAL_TYPE, k -> new ArrayList<>());
                for (String value : valueArray) {
                    String trimmedValue = value.trim();
                    if (!trimmedValue.isEmpty()) {
                        materialList.add(trimmedValue);
                    }
                }
            }
        }

        return filterMap;
    }

    private static String mapFilterType(String filterType) {
        switch (filterType.toLowerCase()) {
            case "display":
                return DISPLAY;
            case "lore":
                return LORE;
            default:
                return filterType;
        }
    }

}