package io.github.insideranh.stellarprotect.callback;

import lombok.Getter;

@Getter
public class CallbackLookup<A, B> {

    private final A logs;
    private final B total;

    public CallbackLookup(A logs, B total) {
        this.logs = logs;
        this.total = total;
    }

}