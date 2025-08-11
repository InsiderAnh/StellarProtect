package io.github.insideranh.stellarprotect.hooks.tasks;

public class TaskCanceller {

    private final Runnable canceller;

    public TaskCanceller(Runnable canceller) {
        this.canceller = canceller;
    }

    public void cancel() {
        canceller.run();
    }

}