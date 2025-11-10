package io.github.insideranh.stellarprotect.utils;

import io.github.insideranh.stellarprotect.StellarProtect;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

@Getter
public class UpdateChecker {

    private String version = "none";
    private boolean isNewVersion = false;

    public UpdateChecker() {
        String currentVersion = StellarProtect.getInstance().getDescription().getVersion();
        StellarProtect.getInstance().getStellarTaskHook(() -> {
            try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=127280/~").openStream();
                 Scanner scanner = new Scanner(is)) {
                if (scanner.hasNext()) {
                    version = scanner.next();
                    isNewVersion = isNewerVersion(currentVersion, version);
                }
            } catch (IOException e) {
                StellarProtect.getInstance().getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        }).runTask();
    }

    private boolean isNewerVersion(String currentVersion, String remoteVersion) {
        if (currentVersion.equals(remoteVersion)) {
            return false;
        }

        try {
            String[] currentParts = splitVersionAndSuffix(currentVersion);
            String[] remoteParts = splitVersionAndSuffix(remoteVersion);

            String currentNumeric = currentParts[0];
            String currentSuffix = currentParts[1];
            String remoteNumeric = remoteParts[0];
            String remoteSuffix = remoteParts[1];

            int numericComparison = compareNumericVersion(currentNumeric, remoteNumeric);

            if (numericComparison > 0) {
                return false;
            } else if (numericComparison < 0) {
                return true;
            } else {
                return compareSuffix(currentSuffix, remoteSuffix) < 0;
            }
        } catch (Exception e) {
            StellarProtect.getInstance().getLogger().warning("Error comparing versions: " + e.getMessage());
            return !currentVersion.equals(remoteVersion);
        }
    }

    private String[] splitVersionAndSuffix(String version) {
        String numeric = version.replaceAll("[^0-9.]", "");
        String suffix = version.replaceAll("[0-9.]", "");
        return new String[]{numeric, suffix};
    }

    private int compareNumericVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int compareSuffix(String s1, String s2) {
        if (s1.isEmpty() && !s2.isEmpty()) return 1;
        if (!s1.isEmpty() && s2.isEmpty()) return -1;
        if (s1.isEmpty() && s2.isEmpty()) return 0;

        return s1.compareTo(s2);
    }

    public void sendUpdateMessage(Player player) {
        if (version.equals("none") || !isNewVersion) return;

        String currentVersion = StellarProtect.getInstance().getDescription().getVersion();

        TextComponent textComponent = new TextComponent("§6§lStellarProtect §8| §fNew version available: §a" + version + " §fYour: §c" + currentVersion + " ");
        TextComponent download = new TextComponent("§6[Download]");
        download.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§fClick to download")));
        download.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/plugin/stellarprotect"));
        TextComponent changelog = new TextComponent(" §6[Changelog]");
        changelog.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§fClick to see changelog")));
        changelog.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth.com/plugin/stellarprotect/changelog"));
        textComponent.addExtra(download);
        textComponent.addExtra(changelog);

        player.spigot().sendMessage(textComponent);
    }

}