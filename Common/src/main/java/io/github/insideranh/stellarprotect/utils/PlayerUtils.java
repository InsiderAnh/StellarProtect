package io.github.insideranh.stellarprotect.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import lombok.NonNull;
import org.bukkit.entity.EntityType;

import java.util.concurrent.atomic.AtomicLong;

public class PlayerUtils {

    private static final BiMap<String, Long> specialIds = HashBiMap.create();

    // ID = -1 = Console
    static {
        specialIds.put("=natural", -2L);
        specialIds.put("=fire", -3L);
        specialIds.put("=water", -4L);
        specialIds.put("=lava", -5L);
        specialIds.put("=ice_melt", -6L);
        specialIds.put("=snow_fall", -7L);
        specialIds.put("=lightning", -8L);
        specialIds.put("=dripstone", -9L);

        specialIds.put("=piston", -10L);
        specialIds.put("=explosion", -11L);
        specialIds.put("=redstone", -12L);
        specialIds.put("=gravity", -13L);
        specialIds.put("=dispenser", -14L);
        specialIds.put("=observer", -15L);
        specialIds.put("=decay", -16L);
        specialIds.put("=portal", -17L);
    }

    public static void loadEntityIds() {
        AtomicLong entityId = new AtomicLong(-200L);
        for (EntityType entityType : EntityType.values()) {
            String keyId = "=" + entityType.name().toLowerCase();
            if (specialIds.containsKey(keyId)) {
                continue;
            }
            long id = entityId.getAndDecrement();
            if (specialIds.containsValue(id)) {
                continue;
            }
            specialIds.put(keyId, id);

            StellarProtect.getInstance().getProtectDatabase().saveEntityId(keyId, id);
        }
    }

    public static void cacheEntityId(String entityType, long entityId) {
        specialIds.put(entityType, entityId);
    }

    public static long getPlayerOrEntityId(String name) {
        long id = PlayerProtect.getPlayerId(name);
        if (id != -2) {
            return id;
        }
        long directId = getEntityByDirectId(name);
        if (directId != -2L) {
            return directId;
        }
        return getSearchByDirectId(name);
    }

    public static long getEntityByDirectId(String entityType) {
        if (specialIds.containsKey(entityType)) {
            return specialIds.get(entityType);
        }
        return -2L;
    }

    public static long getSearchByDirectId(String entityType) {
        String keyId = "=" + entityType.toLowerCase();

        if (specialIds.containsKey(keyId)) {
            return specialIds.get(keyId);
        }
        return -2L;
    }

    @NonNull
    public static String getEntityType(long entityId) {
        if (specialIds.inverse().containsKey(entityId)) {
            return specialIds.inverse().get(entityId);
        }
        return "=none";
    }

}