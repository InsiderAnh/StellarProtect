package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum DeathCause {

    KILL(0),
    WORLD_BORDER(1),
    CONTACT(2),
    ENTITY_ATTACK(3),
    ENTITY_SWEEP_ATTACK(4),
    PROJECTILE(4),
    SUFFOCATION(5),
    FALL(6),
    FIRE(7),
    FIRE_TICK(8),
    MELTING(9),
    LAVA(10),
    DROWNING(11),
    BLOCK_EXPLOSION(12),
    ENTITY_EXPLOSION(13),
    VOID(14),
    LIGHTNING(15),
    SUICIDE(16),
    STARVATION(17),
    POISON(18),
    MAGIC(19),
    WITHER(20),
    FALLING_BLOCK(21),
    THORNS(22),
    DRAGON_BREATH(23),
    CUSTOM(24),
    FLY_INTO_WALL(25),
    HOT_FLOOR(26),
    CAMPFIRE(27),
    CRAMMING(28),
    DRYOUT(29),
    FREEZE(30),
    SONIC_BOOM(31),
    UNKNOWN(100);

    private byte id;

    DeathCause(int id) {
        this.id = (byte) id;
    }

    public static DeathCause getById(byte id) {
        for (DeathCause cause : DeathCause.values()) {
            if (cause.getId() == id) {
                return cause;
            }
        }
        return DeathCause.UNKNOWN;
    }

    public static DeathCause getById(String id) {
        try {
            return DeathCause.valueOf(id);
        } catch (IllegalArgumentException e) {
            return DeathCause.UNKNOWN;
        }
    }
}