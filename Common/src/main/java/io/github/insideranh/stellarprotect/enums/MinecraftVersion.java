package io.github.insideranh.stellarprotect.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum MinecraftVersion {

    v1_8,
    v1_9,
    v1_10,
    v1_11,
    v1_12,
    v1_13,
    v1_14,
    v1_15,
    v1_16,
    v1_17,
    v1_18_R1,
    v1_18_R2,
    v1_19_R1,
    v1_19_R2,
    v1_19_R3,
    v1_20_R1("1.20.1-R0.1-SNAPSHOT"),
    v1_20_R2("1.20.2-R0.1-SNAPSHOT"),
    v1_20_R3("1.20.3-R0.1-SNAPSHOT", "1.20.4-R0.1-SNAPSHOT"),
    v1_20_R4("1.20.5-R0.1-SNAPSHOT", "1.20.6-R0.1-SNAPSHOT"),
    v1_20(false),
    v1_21_R1("1.21.1-R0.1-SNAPSHOT"),
    v1_21_R2("1.21.2-R0.1-SNAPSHOT"),
    v1_21_R3("1.21.3-R0.1-SNAPSHOT"),
    v1_21_R4("1.21.4-R0.1-SNAPSHOT"),
    v1_21_R5("1.21.5-R0.1-SNAPSHOT"),
    v1_21_R6("1.21.6-R0.1-SNAPSHOT"),
    v1_21_R7("1.21.7-R0.1-SNAPSHOT"),
    v1_21_R8("1.21.8-R0.1-SNAPSHOT"),
    v1_21_R9("1.21.9-R0.1-SNAPSHOT"),
    v1_21_R10("1.21.10-R0.1-SNAPSHOT"),
    v1_21_R11("1.21.11-R0.1-SNAPSHOT"),
    v1_21("1.21-R0.1-SNAPSHOT"),
    v1_22;

    private final Set<String> versions;
    private final boolean implemented;

    MinecraftVersion() {
        this(true);
    }

    MinecraftVersion(boolean implemented) {
        this.implemented = implemented;
        this.versions = new HashSet<>();
    }

    MinecraftVersion(String... versions) {
        this.implemented = true;
        this.versions = new HashSet<>(Arrays.asList(versions));
    }

    MinecraftVersion(boolean implemented, String... versions) {
        this.implemented = implemented;
        this.versions = new HashSet<>(Arrays.asList(versions));
    }

    public static MinecraftVersion get(String v) {
        String replaced = v.replace('.', '_');

        for (MinecraftVersion k : MinecraftVersion.values()) {
            if (replaced.startsWith(k.name()) || replaced.startsWith(k.name().substring(1)) || k.versions.contains(v)) {
                return k;
            }
        }
        return null;
    }

    // Would be really cool if we could overload operators here
    public boolean greaterThanOrEqualTo(MinecraftVersion other) {
        return ordinal() >= other.ordinal();
    }

    public boolean lessThanOrEqualTo(MinecraftVersion other) {
        return ordinal() <= other.ordinal();
    }

}