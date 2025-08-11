package io.github.insideranh.stellarprotect.config.suspicious;

import io.github.insideranh.stellarprotect.config.PatternConfig;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

@Getter
public class TpaSuspiciousConfig extends PatternConfig {

    private final long expireCommandTpa;
    private final List<String> checkCommands;

    public TpaSuspiciousConfig(FileConfiguration config, String path) {
        super(config, path);

        this.expireCommandTpa = config.getInt(path + ".expire-command-tpa") * 1000L;
        this.checkCommands = config.getStringList(path + ".check-commands");
    }

}