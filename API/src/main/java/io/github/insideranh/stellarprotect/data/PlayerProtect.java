package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.maps.ObjectLongMap;
import io.github.insideranh.stellarprotect.maps.ObjectObjectMap;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
public class PlayerProtect {

    private static final ObjectObjectMap<UUID, PlayerProtect> players = new ObjectObjectMap<>(120);
    private static final ObjectLongMap<String> nameToIdCache = new ObjectLongMap<>(120);

    private final long playerId;
    private final UUID uuid;
    private final String name;
    private final String realName;

    private LookupSession lookupSession;
    private InspectSession inspectSession;

    private boolean inspect;

    private long nextLookup;
    private long nextUse;
    private long nextInspect;
    private long loginTime;

    private double lastEconomyBalance;

    private Location lastLocation;
    private int lastPickUpAmount;
    private long pickUpXYZ;
    private long lastPickItemId;
    private long nextSeparateLogPickUp;

    private HashMap<Integer, Object> posibleLogs = new HashMap<>();

    public PlayerProtect(UUID uuid, String name, long playerId) {
        this.playerId = playerId;
        this.name = name.toLowerCase();
        this.realName = name;
        this.uuid = uuid;
    }

    public static PlayerProtect getPlayer(Player player) {
        return players.get(player.getUniqueId());
    }

    public static PlayerProtect removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProtect removed = players.remove(uuid);
        if (removed != null) {
            nameToIdCache.removeLong(removed.name);
        }
        return removed;
    }

    public static long getPlayerId(String name) {
        String lowerName = name.toLowerCase();

        if (nameToIdCache.containsKey(lowerName)) {
            return nameToIdCache.getLong(lowerName);
        }

        for (PlayerProtect playerProtect : players.values()) {
            if (playerProtect.name.equals(lowerName)) {
                nameToIdCache.put(lowerName, playerProtect.playerId);
                return playerProtect.playerId;
            }
        }

        return -2;
    }

    public void create() {
        players.put(uuid, this);
        nameToIdCache.put(name, playerId);
    }

}