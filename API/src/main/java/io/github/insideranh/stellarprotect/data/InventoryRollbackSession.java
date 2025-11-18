package io.github.insideranh.stellarprotect.data;

import io.github.insideranh.stellarprotect.arguments.DatabaseFilters;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
@Setter
public class InventoryRollbackSession {

    private final Player player;
    private final DatabaseFilters databaseFilters;
    private boolean verbose;
    private boolean silent;
    private boolean active;

    public InventoryRollbackSession(Player player, DatabaseFilters databaseFilters, boolean verbose, boolean silent) {
        this.player = player;
        this.databaseFilters = databaseFilters;
        this.verbose = verbose;
        this.silent = silent;
        this.active = true;
    }

    public void toggle() {
        this.active = !this.active;
    }

    public void deactivate() {
        this.active = false;
    }

}

