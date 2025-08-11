package io.github.insideranh.stellarprotect.enums;

import lombok.Getter;

@Getter
public enum TimesType {

    YEAR("y", 31536000),
    MONTH("mo", 2592000),
    WEEK("w", 604800),
    DAY("d", 86400),
    HOUR("h", 3600),
    MINUTE("m", 60),
    SECOND("s", 1);

    private final String symbol;
    private final int seconds;

    TimesType(String symbol, int seconds) {
        this.symbol = symbol;
        this.seconds = seconds;
    }

}