package io.github.insideranh.stellarprotect.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtils {

    private static final long MINUTE = 60;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY;
    private static final long YEAR = 365 * DAY;

    public static String formatMillisAsCompactDHMS(long millis) {
        if (millis < 1000L) {
            return "0s";
        }

        long years = millis / 31536000000L;
        millis %= 31536000000L;
        long months = millis / 2592000000L;
        millis %= 2592000000L;
        long days = millis / 86400000L;
        millis %= 86400000L;
        long hours = millis / 3600000L;
        millis %= 3600000L;
        long minutes = millis / 60000L;
        millis %= 60000L;
        long seconds = millis / 1000L;

        StringBuilder time = new StringBuilder();
        int unitsAdded = 0;

        if (years > 0) {
            time.append(years).append("y");
            unitsAdded++;
        }
        if (months > 0) {
            time.append(months).append("mo");
            unitsAdded++;
        }
        if (days > 0 && unitsAdded < 2) {
            time.append(days).append("d");
            unitsAdded++;
        }
        if (hours > 0 && unitsAdded < 2) {
            time.append(String.format("%02d", hours)).append("h");
            unitsAdded++;
        }
        if (minutes > 0 && unitsAdded < 2) {
            time.append(String.format("%02d", minutes)).append("m");
            unitsAdded++;
        }
        if (seconds > 0 && unitsAdded < 2) {
            time.append(String.format("%02d", seconds)).append("s");
        }

        return time.toString().trim();
    }

    public String formatMillisAsAgo(long createdAt) {
        long currentTime = System.currentTimeMillis();
        long timeAgo = currentTime - createdAt;
        return formatDuration(timeAgo);
    }

    public static String formatDuration(long durationMillis) {
        long totalSeconds = durationMillis / 1000;

        if (totalSeconds < MINUTE) {
            return String.format("00:%02d/s ago", totalSeconds);
        } else if (totalSeconds < HOUR) {
            long minutes = totalSeconds / MINUTE;
            long seconds = totalSeconds % MINUTE;
            return String.format("%02d:%02d/m ago", minutes, seconds);
        } else if (totalSeconds < DAY) {
            long hours = totalSeconds / HOUR;
            long minutes = (totalSeconds % HOUR) / MINUTE;
            return String.format("%02d:%02d/h ago", hours, minutes);
        } else if (totalSeconds < WEEK) {
            long days = totalSeconds / DAY;
            long hours = (totalSeconds % DAY) / HOUR;
            return String.format("%02d:%02d/d ago", days, hours);
        } else if (totalSeconds < MONTH) {
            long weeks = totalSeconds / WEEK;
            long days = (totalSeconds % WEEK) / DAY;
            return String.format("%02d:%02d/w ago", weeks, days);
        } else if (totalSeconds < YEAR) {
            long months = totalSeconds / MONTH;
            long weeks = (totalSeconds % MONTH) / WEEK;
            return String.format("%02d:%02d/M ago", months, weeks);
        } else {
            long years = totalSeconds / YEAR;
            long months = (totalSeconds % YEAR) / MONTH;
            return String.format("%02d:%02d/y ago", years, months);
        }
    }

}