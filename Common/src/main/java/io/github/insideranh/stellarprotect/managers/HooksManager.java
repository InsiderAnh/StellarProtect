package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.settings.InsiderConfig;
import lombok.Getter;

@Getter
public class HooksManager {

    private final StellarProtect plugin = StellarProtect.getInstance();
    private InsiderConfig config;

    private boolean shopGuiHook;
    private boolean nexoHook;
    private boolean xPlayerKitsHook;

    public void load() {
        if (config == null) {
            config = new InsiderConfig(plugin, "hooks", true, false);
        } else {
            config.reload();
        }

        shopGuiHook = config.getBoolean("hooks.shop_gui.enabled");
        nexoHook = config.getBoolean("hooks.nexo.enabled");
        xPlayerKitsHook = config.getBoolean("hooks.xplayerkits.enabled");
    }

}