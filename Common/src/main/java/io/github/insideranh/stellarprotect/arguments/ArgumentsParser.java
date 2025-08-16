package io.github.insideranh.stellarprotect.arguments;

import com.mongodb.lang.Nullable;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.enums.ActionType;
import io.github.insideranh.stellarprotect.enums.TimesType;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgumentsParser {

    private static final Pattern TIME_PATTERN = Pattern.compile("([\\d.]+)(mo|[ywdhms])");

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
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

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

    public static List<String> parseIncludesWord(String[] arguments) {
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

    public static List<String> parseExcludesWord(String[] arguments) {
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
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("t:") || part.startsWith("time:")) {
                String timeSegment = part.replaceFirst("^(t:|time:)", "");
                String[] rangeParts = timeSegment.split("-");

                if (rangeParts.length == 0) {
                    return new TimeArg("", "", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), System.currentTimeMillis());
                }

                String startString = rangeParts[0];
                String endString = rangeParts.length > 1 ? rangeParts[1] : "";

                long startDurationMs = parseDuration(rangeParts[0]) * 1000L;
                long endDurationMs = rangeParts.length > 1 ? parseDuration(rangeParts[1]) * 1000L : 0;

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

    public static @Nullable RadiusArg parseRadiusOrNull(String[] arguments, @NonNull Location location) {
        String joined = String.join(" ", arguments).toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "").replace(",", "");

        for (String part : joined.split("\\s+")) {
            if (part.startsWith("r:") || part.startsWith("radius:")) {
                String radiusSegment = part.replaceFirst("^(r:|radius:)", "");
                if (radiusSegment.isEmpty()) continue;

                try {
                    double radius = Double.parseDouble(radiusSegment);
                    return new RadiusArg(radius,
                        location.getBlockX() - radius,
                        location.getBlockX() + radius,
                        location.getBlockY() - radius,
                        location.getBlockY() + radius,
                        location.getBlockZ() - radius,
                        location.getBlockZ() + radius);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

}