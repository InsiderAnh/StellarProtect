package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.settings.InsiderConfig;
import io.github.insideranh.stellarprotect.utils.LocationUtils;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.Function;

@Getter
public class LangManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private InsiderConfig lang;

    public void load() {
        if (lang == null) {
            this.lang = new InsiderConfig(plugin, "lang", true, false);
        } else {
            this.lang.reload();
        }

        LocationUtils.FORMAT_LOCATION = lang.getStringOrDefault("messages.actions.locationFormat", "x<x>/y<y>/z<z>/<world>");
    }

    public void sendMessage(@NonNull CommandSender sender, @NonNull String path) {
        sendMessage(sender, path, text -> text);
    }

    public void sendMessage(@NonNull CommandSender sender, @NonNull String path, Function<String, String> replacer) {
        sender.sendMessage(replacer.apply(lang.getString(path)));
    }

    public String get(@NonNull String path) {
        return lang.getString(path);
    }

    public List<String> getList(@NonNull String path) {
        return lang.getList(path);
    }

    public String get(@NonNull String path, Function<String, String> replacer) {
        return replacer.apply(lang.getString(path));
    }

}