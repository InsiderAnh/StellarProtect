package io.github.insideranh.stellarprotect.callback;

import lombok.Getter;

@Getter
public class CallbackSource<A, B> {

    private final A location;
    private final B playerId;

    public CallbackSource(A location, B playerId) {
        this.location = location;
        this.playerId = playerId;
    }

}