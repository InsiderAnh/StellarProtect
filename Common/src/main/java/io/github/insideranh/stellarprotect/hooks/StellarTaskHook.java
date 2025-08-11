package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.hooks.tasks.TaskCanceller;
import org.bukkit.Location;

public abstract class StellarTaskHook {

    public final Runnable runnable;

    protected StellarTaskHook(Runnable runnable) {
        this.runnable = runnable;
    }

    public abstract TaskCanceller runTask(Location location);

    public abstract TaskCanceller runTask(Location location, long delay);

    public abstract TaskCanceller runTask();

    public abstract TaskCanceller runTask(long delay);

    public abstract TaskCanceller runTaskTimer(long delay, long period);

    public abstract TaskCanceller runTaskTimerAsynchronously(long delay, long period);

    public abstract TaskCanceller runTaskLaterAsynchronously(long delay);

}