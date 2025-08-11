package io.github.insideranh.stellarprotect.nms.v1_21_R7;

import io.github.insideranh.stellarprotect.api.ColorUtils;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ColorUtils_v1_21_R7 extends ColorUtils {

    private final Pattern pattern = Pattern.compile("&?#[a-fA-F0-9]{6}");

    public List<String> color(List<String> message) {
        return message.stream().map(this::color).collect(Collectors.toList());
    }

    public String color(String message) {
        return translateAlternateColorCodes('&', message);
    }

    public String translateAlternateColorCodes(char altColorChar, String message) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            try {
                ChatColor chatColor = ChatColor.of(color.replaceFirst("&", ""));
                matcher.appendReplacement(stringBuffer, chatColor.toString());
            } catch (Exception ignored) {
            }
        }
        matcher.appendTail(stringBuffer);
        message = stringBuffer.toString();
        return ChatColor.translateAlternateColorCodes(altColorChar, message);
    }

}