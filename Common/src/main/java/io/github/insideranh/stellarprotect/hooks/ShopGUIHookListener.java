package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.LoggerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.entries.hooks.PlayerShopGUIEntry;
import io.github.insideranh.stellarprotect.items.ItemReference;
import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopTransactionResult;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ShopGUIHookListener implements Listener {

    private final StellarProtect plugin = StellarProtect.getInstance();

    @EventHandler
    public void onPostTransaction(ShopPostTransactionEvent event) {
        ShopTransactionResult result = event.getResult();
        if (!result.getResult().equals(ShopTransactionResult.ShopTransactionResultType.SUCCESS)) return;

        Player player = result.getPlayer();
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) return;

        ShopItem shopItem = result.getShopItem();

        ItemReference itemReference = plugin.getItemsManager().getItemReference(shopItem.getItem());

        int amount = result.getAmount();
        double price = result.getPrice();

        LoggerCache.addLog(new PlayerShopGUIEntry(playerProtect.getPlayerId(), player.getLocation(), itemReference, amount, price, (byte) result.getShopAction().ordinal()));
    }

}