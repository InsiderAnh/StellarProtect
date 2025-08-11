package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum SuspiciousType {

    TPA_KILL(0);

    private final int id;

    SuspiciousType(int id) {
        this.id = id;
    }

}