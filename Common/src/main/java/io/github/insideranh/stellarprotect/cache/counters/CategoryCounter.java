package io.github.insideranh.stellarprotect.cache.counters;

import java.util.concurrent.atomic.AtomicInteger;

public class CategoryCounter {

    private final AtomicInteger count = new AtomicInteger(0);
    private final int threshold;

    public CategoryCounter(int threshold) {
        this.threshold = threshold;
    }

    public boolean incrementAndCheckThreshold() {
        return count.incrementAndGet() >= threshold;
    }

    public void reset() {
        count.set(0);
    }

    public int get() {
        return count.get();
    }

}