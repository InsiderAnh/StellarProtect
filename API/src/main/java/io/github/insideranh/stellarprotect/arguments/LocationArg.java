package io.github.insideranh.stellarprotect.arguments;

import lombok.Getter;

@Getter
public class LocationArg {

    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public LocationArg(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

}