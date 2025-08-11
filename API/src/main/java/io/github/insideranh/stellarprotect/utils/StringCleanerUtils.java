package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.items.MinecraftItem;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringCleanerUtils {

    public static final String COMMON_BASE64 = "rO0ABXNyABpvcmcuYnVra2l0LnV0aWwuaW8uV3JhcHBlcvJQR+zxEm8FAgABTAADbWFwdAAPTGph" +
        "dmEvdXRpbC9NYXA7eHBzcgA1Y29tLmdvb2dsZS5jb21tb24uY29sbGVjdC5JbW11dGFibGVNYXAk" +
        "U2VyaWFsaXplZEZvcm0AAAAAAAAAAAIAAkwABGtleXN0ABJMamF2YS9sYW5nL09iamVjdDtMAAZ2" +
        "YWx1ZXNxAH4ABHhwdXIAE1tMamF2YS5sYW5nLk9iamVjdDuQzlifEHMpbAIAAHhwAAAABXQAAj09" +
        "dAALRGF0YVZlcnNpb250AAJpZHQABWNvdW50dAAOc2NoZW1hX3ZlcnNpb251cQB+AAYAAAAFdAAe" +
        "b3JnLmJ1a2tpdC5pbnZlbnRvcnkuSXRlbVN0YWNrc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeB";
    private static final DecimalFormat format = new DecimalFormat("###,###,###.##");
    private static final NavigableMap<Long, String> SUFFIXES = new TreeMap<>();
    // This is the base64 of the item "public static bukkit item bla bla bla"
    static Pattern pattern = Pattern.compile("\"m\":\"([^\"]+)\"");

    static {
        SUFFIXES.put(1_000L, "k");
        SUFFIXES.put(1_000_000L, "M");
        SUFFIXES.put(1_000_000_000L, "B");
        SUFFIXES.put(1_000_000_000_000L, "T");
        SUFFIXES.put(1_000_000_000_000_000L, "Q");
        SUFFIXES.put(1_000_000_000_000_000_000L, "Qi");
    }

    public static double limitTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static String formatNumber(double amount) {
        return format.format(amount);
    }

    public static String formatEconomy(double value) {
        if (value == Double.MIN_VALUE) return formatEconomy(Double.MIN_VALUE + 1);
        if (value < 0) return "-" + formatEconomy(-value);
        if (value < 1000) return formatNumber(value);
        Map.Entry<Long, String> e = SUFFIXES.floorEntry((long) value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();
        long truncated = (long) (value / (divideBy / 10));
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10d);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    private static String cleanMinecraftName(String name) {
        if (name == null || name.length() < 2) return name;

        String cleanedData = cleanMinecraft(name);

        int bracketIndex = cleanedData.indexOf('[');
        if (bracketIndex != -1) {
            cleanedData = cleanedData.substring(0, bracketIndex);
        }

        StringBuilder builder = new StringBuilder();
        for (String word : cleanedData.split("_")) {
            if (!word.isEmpty()) {
                builder.append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
            }
        }

        return builder.toString().trim();
    }

    private static String cleanMinecraft(String name) {
        if (name.startsWith("minecraft:")) {
            return name.replaceFirst("minecraft:", "");
        }
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return name;
    }

    public static MinecraftItem parseMinecraftData(String data) {
        if (data == null || data.isEmpty()) {
            return new MinecraftItem("", new HashMap<>());
        }

        String itemName;
        Map<String, String> properties = new HashMap<>();

        int bracketIndex = data.indexOf('[');

        if (bracketIndex != -1) {
            itemName = data.substring(0, bracketIndex);
            String propertiesString = data.substring(bracketIndex + 1, data.lastIndexOf(']'));

            if (!propertiesString.isEmpty()) {
                String[] pairs = propertiesString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.trim().split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        key = key.substring(0, 1).toUpperCase() + key.substring(1);
                        value = value.substring(0, 1).toUpperCase() + value.substring(1);

                        properties.put(key, value);
                    }
                }
            }
        } else {
            itemName = data;
        }

        String cleanName = cleanMinecraftName(itemName);

        return new MinecraftItem(cleanName, properties);
    }

}