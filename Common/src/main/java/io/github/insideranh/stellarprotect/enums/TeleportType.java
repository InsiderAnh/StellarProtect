package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum TeleportType {

    ENDER_PEARL(0),
    COMMAND(1),
    PLUGIN(2),
    NETHER_PORTAL(3),
    END_PORTAL(4),
    SPECTATE(5),
    END_GATEWAY(6),
    CHORUS_FRUIT(7),
    DISMOUNT(8),
    EXIT_BED(9),
    UNKNOWN(10);

    private final int id;

    TeleportType(int id) {
        this.id = id;
    }

    public static TeleportType getByName(String name) {
        for (TeleportType teleportType : TeleportType.values()) {
            if (teleportType.name().equalsIgnoreCase(name)) {
                return teleportType;
            }
        }
        return TeleportType.UNKNOWN;
    }

    public static TeleportType getById(int id) {
        for (TeleportType teleportType : TeleportType.values()) {
            if (teleportType.getId() == id) {
                return teleportType;
            }
        }
        return TeleportType.UNKNOWN;
    }

}