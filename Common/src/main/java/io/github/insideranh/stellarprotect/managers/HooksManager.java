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
    private boolean itemsAdderHook;
    private boolean xPlayerKitsHook;
    private boolean worldEditHook;
    private boolean treeCuterHook;
    private boolean placeholderApiHook;

    public void load() {
        if (config == null) {
            config = new InsiderConfig(plugin, "hooks", true, false);
        } else {
            config.reload();
        }

        this.shopGuiHook = config.getBoolean("hooks.shop_gui.enabled");
        this.nexoHook = config.getBoolean("hooks.nexo.enabled");
        this.itemsAdderHook = config.getBoolean("hooks.itemsAdder.enabled");
        this.xPlayerKitsHook = config.getBoolean("hooks.xplayerkits.enabled");
        this.worldEditHook = config.getBoolean("hooks.worldedit.enabled");
        this.treeCuterHook = config.getBoolean("hooks.treecuter.enabled");
        this.placeholderApiHook = config.getBoolean("hooks.placeholderapi.enabled");
    }

}