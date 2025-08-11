package io.github.insideranh.stellarprotect.nms.v1_8_R3;

import io.github.insideranh.stellarprotect.api.ColorUtils;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class ColorUtils_v1_8_R3 extends ColorUtils {

    public List<String> color(List<String> message) {
        return message.stream().map(this::color).collect(Collectors.toList());
    }

    public String color(String message) {
        return translateAlternateColorCodes('&', message);
    }

    public String translateAlternateColorCodes(char altColorChar, String message) {
        return ChatColor.translateAlternateColorCodes(altColorChar, message);
    }

}