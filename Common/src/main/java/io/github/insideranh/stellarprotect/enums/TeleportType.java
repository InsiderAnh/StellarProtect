package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

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

    private static final TeleportType[] ID_CACHE = new TeleportType[11];
    private static final Map<String, TeleportType> NAME_CACHE;

    static {
        TeleportType[] values = values();

        for (TeleportType type : values) {
            ID_CACHE[type.id] = type;
        }

        NAME_CACHE = new HashMap<>(values.length * 2);

        for (TeleportType type : values) {
            String name = type.name();
            NAME_CACHE.put(name, type);
            NAME_CACHE.put(name.toLowerCase(), type);

            if (name.contains("_")) {
                String spaceName = name.replace("_", " ");
                NAME_CACHE.put(spaceName, type);
                NAME_CACHE.put(spaceName.toLowerCase(), type);
            }
        }
    }

    TeleportType(int id) {
        this.id = id;
    }

    public static TeleportType getByName(String name) {
        if (name == null) {
            return UNKNOWN;
        }

        TeleportType cached = NAME_CACHE.get(name);
        if (cached != null) {
            return cached;
        }

        cached = NAME_CACHE.get(name.toLowerCase());
        if (cached != null) {
            return cached;
        }

        int len = name.length();
        TeleportType[] values = values();

        for (TeleportType type : values) {
            String typeName = type.name();
            if (typeName.length() != len) {
                continue;
            }

            if (equalsIgnoreCase(name, typeName)) {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static TeleportType getById(int id) {
        if (id >= 0 && id < ID_CACHE.length) {
            return ID_CACHE[id];
        }
        return UNKNOWN;
    }

    public static TeleportType getByOrdinal(int ordinal) {
        TeleportType[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return UNKNOWN;
    }

    private static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        }

        for (int i = 0; i < str1.length(); i++) {
            char c1 = str1.charAt(i);
            char c2 = str2.charAt(i);

            if (c1 >= 'A' && c1 <= 'Z') {
                c1 = (char) (c1 | 0x20);
            }
            if (c2 >= 'A' && c2 <= 'Z') {
                c2 = (char) (c2 | 0x20);
            }

            if (c1 != c2) {
                return false;
            }
        }
        return true;
    }

}