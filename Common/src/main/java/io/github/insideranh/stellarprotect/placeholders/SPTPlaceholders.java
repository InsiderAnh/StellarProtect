package io.github.insideranh.stellarprotect.placeholders;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SPTPlaceholders extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "spt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "InsiderAnh";
    }

    @Override
    public @NotNull String getVersion() {
        return StellarProtect.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String placeholder) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (placeholder.equals("inspect_enabled")) {
            return playerProtect != null && playerProtect.isInspect() ? "true" : "false";
        }
        return null;
    }

}