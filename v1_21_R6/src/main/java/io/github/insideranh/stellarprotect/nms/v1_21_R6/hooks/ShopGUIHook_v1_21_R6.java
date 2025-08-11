package io.github.insideranh.stellarprotect.nms.v1_21_R6.hooks;

import io.github.insideranh.stellarprotect.api.events.ShopGuiHookHandler;
import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ShopGUIHook_v1_21_R6 implements Listener {

    private final ShopGuiHookHandler shopGuiHookHandler;

    public ShopGUIHook_v1_21_R6(ShopGuiHookHandler shopGuiHookHandler) {
        this.shopGuiHookHandler = shopGuiHookHandler;
    }

    @EventHandler
    public void onPostTransaction(ShopPostTransactionEvent event) {

    }

}