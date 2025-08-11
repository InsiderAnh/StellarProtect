package io.github.insideranh.stellarprotect.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
public class PlayerProtect {

    private static HashMap<UUID, PlayerProtect> players = new HashMap<>();
    private final long playerId;
    private final UUID uuid;
    private String name;
    private boolean inspect;
    private long nextInspect;
    private LookupSession lookupSession;
    private long nextLookup;
    private InspectSession inspectSession;
    private long nextUse;

    private long loginTime;

    private double lastEconomyBalance;

    public PlayerProtect(UUID uuid, String name, long playerId) {
        this.playerId = playerId;
        this.name = name.toLowerCase();
        this.uuid = uuid;
    }

    public static PlayerProtect removePlayer(Player player) {
        return players.remove(player.getUniqueId());
    }

    public static PlayerProtect getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public static long getPlayerId(String name) {
        for (PlayerProtect playerProtect : players.values()) {
            if (playerProtect.getName().equalsIgnoreCase(name)) {
                return playerProtect.getPlayerId();
            }
        }
        return -2;
    }

    public void create() {
        players.put(getUuid(), this);
    }

}