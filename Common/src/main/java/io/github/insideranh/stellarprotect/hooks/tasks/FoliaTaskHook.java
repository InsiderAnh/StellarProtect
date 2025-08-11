package io.github.insideranh.stellarprotect.hooks.tasks;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.TaskImplementation;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.hooks.StellarTaskHook;
import org.bukkit.Location;

public class FoliaTaskHook extends StellarTaskHook {

    private static final ServerImplementation serverImplementation = new FoliaCompatibility(StellarProtect.getInstance()).getServerImplementation();

    public FoliaTaskHook(Runnable runnable) {
        super(runnable);
    }

    @Override
    public TaskCanceller runTask(Location location) {
        TaskImplementation<Void> task = serverImplementation.region(location).run(runnable);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask(Location location, long delay) {
        TaskImplementation<Void> task = serverImplementation.region(location).runDelayed(runnable, delay);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask() {
        TaskImplementation<Void> task = serverImplementation.async().runNow(runnable);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask(long delay) {
        TaskImplementation<Void> task = serverImplementation.async().runDelayed(runnable, delay);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskTimer(long delay, long period) {
        TaskImplementation<Void> task = serverImplementation.async().runAtFixedRate(runnable, delay, period);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskTimerAsynchronously(long delay, long period) {
        TaskImplementation<Void> task = serverImplementation.async().runAtFixedRate(runnable, delay, period);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskLaterAsynchronously(long delay) {
        TaskImplementation<Void> task = serverImplementation.async().runDelayed(runnable, delay);
        return new TaskCanceller(task::cancel);
    }

}