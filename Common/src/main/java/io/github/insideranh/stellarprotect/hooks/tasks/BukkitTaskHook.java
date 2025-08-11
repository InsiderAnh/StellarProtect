package io.github.insideranh.stellarprotect.hooks.tasks;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.hooks.StellarTaskHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskHook extends StellarTaskHook {

    public BukkitTaskHook(Runnable runnable) {
        super(runnable);
    }

    @Override
    public TaskCanceller runTask(Location location) {
        BukkitTask task = Bukkit.getScheduler().runTask(StellarProtect.getInstance(), runnable);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask(Location location, long delay) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(StellarProtect.getInstance(), runnable, delay);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask() {
        BukkitTask task = Bukkit.getScheduler().runTask(StellarProtect.getInstance(), runnable);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTask(long delay) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(StellarProtect.getInstance(), runnable, delay);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskLaterAsynchronously(long delay) {
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(StellarProtect.getInstance(), runnable, delay);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskTimer(long delay, long period) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(StellarProtect.getInstance(), runnable, delay, period);
        return new TaskCanceller(task::cancel);
    }

    @Override
    public TaskCanceller runTaskTimerAsynchronously(long delay, long period) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(StellarProtect.getInstance(), runnable, delay, period);
        return new TaskCanceller(task::cancel);
    }

}