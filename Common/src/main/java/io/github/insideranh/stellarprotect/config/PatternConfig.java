package io.github.insideranh.stellarprotect.config;

import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public abstract class PatternConfig {

    private final boolean enabled;
    private final String permission;
    private final String message;
    private final String tooltip;

    protected PatternConfig(FileConfiguration config, String path) {
        this.enabled = config.getBoolean(path + ".enabled");
        this.permission = config.getString(path + ".permission");
        this.message = StellarProtect.getInstance().getColorUtils().color(config.getString(path + ".message"));
        this.tooltip = StellarProtect.getInstance().getColorUtils().color(config.getString(path + ".tooltip"));
    }

}