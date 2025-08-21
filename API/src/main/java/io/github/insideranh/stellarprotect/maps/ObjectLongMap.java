package io.github.insideranh.stellarprotect.maps;

import java.util.Arrays;
import java.util.Objects;

public final class ObjectLongMap<K> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private static final long EMPTY_VALUE = Long.MIN_VALUE;

    private Object[] keys;
    private long[] values;
    private boolean[] occupied;
    private int size;
    private int threshold;
    private int mask;

    public ObjectLongMap(int initialCapacity) {
        int capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
        if (capacity < DEFAULT_CAPACITY) capacity = DEFAULT_CAPACITY;

        this.keys = new Object[capacity];
        this.values = new long[capacity];
        this.occupied = new boolean[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
    }

    private static int hash(Object key) {
        int h = key.hashCode();
        h ^= h >>> 16;
        h *= 0x45d9f3b;
        h ^= h >>> 16;
        return h;
    }

    public long getLong(Object key) {
        if (key == null) return EMPTY_VALUE;

        int idx = hash(key) & mask;

        while (occupied[idx]) {
            if (Objects.equals(keys[idx], key)) {
                return values[idx];
            }
            idx = (idx + 1) & mask;
        }
        return EMPTY_VALUE;
    }

    public boolean containsKey(Object key) {
        if (key == null) return false;

        int idx = hash(key) & mask;

        while (occupied[idx]) {
            if (Objects.equals(keys[idx], key)) {
                return true;
            }
            idx = (idx + 1) & mask;
        }
        return false;
    }

    public long put(K key, long value) {
        if (key == null) throw new NullPointerException();

        if (size >= threshold) {
            resize();
        }

        int idx = hash(key) & mask;

        while (occupied[idx]) {
            if (Objects.equals(keys[idx], key)) {
                long oldValue = values[idx];
                values[idx] = value;
                return oldValue;
            }
            idx = (idx + 1) & mask;
        }

        keys[idx] = key;
        values[idx] = value;
        occupied[idx] = true;
        size++;
        return EMPTY_VALUE;
    }

    public long removeLong(Object key) {
        if (key == null) return EMPTY_VALUE;

        int idx = hash(key) & mask;

        while (occupied[idx]) {
            if (Objects.equals(keys[idx], key)) {
                long oldValue = values[idx];

                keys[idx] = null;
                occupied[idx] = false;

                int nextIdx = (idx + 1) & mask;
                while (occupied[nextIdx]) {
                    Object moveKey = keys[nextIdx];
                    long moveValue = values[nextIdx];

                    keys[nextIdx] = null;
                    occupied[nextIdx] = false;

                    int moveIdx = hash(moveKey) & mask;
                    while (occupied[moveIdx]) {
                        moveIdx = (moveIdx + 1) & mask;
                    }
                    keys[moveIdx] = moveKey;
                    values[moveIdx] = moveValue;
                    occupied[moveIdx] = true;

                    nextIdx = (nextIdx + 1) & mask;
                }

                size--;
                return oldValue;
            }
            idx = (idx + 1) & mask;
        }
        return EMPTY_VALUE;
    }

    private void resize() {
        Object[] oldKeys = keys;
        long[] oldValues = values;
        boolean[] oldOccupied = occupied;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new Object[newCapacity];
        values = new long[newCapacity];
        occupied = new boolean[newCapacity];

        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldOccupied[i]) {
                put((K) oldKeys[i], oldValues[i]);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(occupied, false);
        size = 0;
    }

}