package io.github.insideranh.stellarprotect.data;

import lombok.Getter;
import org.bukkit.Location;

@Getter
public class InspectSession {

    private final Location location;
    private final int skip;
    private final int limit;
    private final boolean inspectChest;

    public InspectSession(Location location, int skip, int limit, boolean inspectChest) {
        this.location = location;
        this.skip = skip;
        this.limit = limit;
        this.inspectChest = inspectChest;
    }

    public String getArgument() {
        return this.location.getWorld().getName() + ";" + this.location.getBlockX() + ";" + this.location.getBlockY() + ";" + this.location.getBlockZ();
    }

}