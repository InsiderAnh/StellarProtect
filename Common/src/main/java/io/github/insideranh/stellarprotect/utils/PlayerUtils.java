package io.github.insideranh.stellarprotect.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.cache.PlayerCache;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import lombok.NonNull;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerUtils {

    private static final BiMap<String, Long> specialIds = HashBiMap.create();
    private static long lastLogId = 0;

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
        specialIds.put("=tree", -18L);
        specialIds.put("=vine", -19L);
        specialIds.put("=sculk", -20L);
        specialIds.put("=chorus", -21L);
        specialIds.put("=bamboo", -22L);
        specialIds.put("=amethyst", -23L);
        specialIds.put("=frost_walker", -24L);
        specialIds.put("=dragon", -25L);
        specialIds.put("=warden", -26L);
        specialIds.put("=evoker", -27L);
        specialIds.put("=ravager", -28L);

        specialIds.put("=creeper", -29L);
        specialIds.put("=tnt", -30L);
        specialIds.put("=ghast", -31L);
        specialIds.put("=end_crystal", -32L);
        specialIds.put("=wither_skull", -33L);
        specialIds.put("=minecart_tnt", -34L);
        specialIds.put("=fireball", -35L);

        specialIds.put("=worldedit", -1000L);
    }

    public static long getNextLogId() {
        return ++lastLogId;
    }

    public static void setNextLogId(long id) {
        lastLogId = id;
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

    public static long getPlayerOrConsoleId(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return -1L;
        }
        Player player = (Player) sender;
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        if (playerProtect == null) {
            return -2L;
        }
        return playerProtect.getPlayerId();
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

    public static Set<Long> getExplosionRelatedIds() {
        Set<Long> explosionIds = new HashSet<>();

        if (specialIds.containsKey("=explosion")) {
            explosionIds.add(specialIds.get("=explosion"));
        }

        String[] explosiveEntities = {
            "=creeper", "=wither", "=ghast", "=tnt",
            "=end_crystal", "=wither_skull", "=minecart_tnt"
        };

        for (String entity : explosiveEntities) {
            if (specialIds.containsKey(entity)) {
                explosionIds.add(specialIds.get(entity));
            }
        }

        return explosionIds;
    }

    public static boolean isExplosiveEntity(String userName) {
        if (userName == null || userName.isEmpty()) {
            return false;
        }

        String normalized = userName.toLowerCase().startsWith("=")
            ? userName.toLowerCase()
            : "=" + userName.toLowerCase();

        return normalized.equals("=explosion")
            || normalized.equals("=creeper")
            || normalized.equals("=wither")
            || normalized.equals("=ghast")
            || normalized.equals("=tnt")
            || normalized.equals("=end_crystal")
            || normalized.equals("=wither_skull")
            || normalized.equals("=minecart_tnt")
            || normalized.equals("=fireball");
    }

    public static String getNameOfEntity(long entityId) {
        if (entityId < 0) {
            return getEntityType(entityId);
        }
        return PlayerCache.getName(entityId);
    }

    @NonNull
    public static String getEntityType(long entityId) {
        if (specialIds.inverse().containsKey(entityId)) {
            return specialIds.inverse().get(entityId);
        }
        return "=none";
    }

}