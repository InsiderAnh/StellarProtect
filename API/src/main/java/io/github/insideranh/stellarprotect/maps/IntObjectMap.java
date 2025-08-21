package io.github.insideranh.stellarprotect.maps;

import java.util.Arrays;

public final class IntObjectMap<V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int EMPTY_KEY = Integer.MIN_VALUE;

    private int[] keys;
    private Object[] values;
    private int size;
    private int threshold;
    private int mask;

    public IntObjectMap(int initialCapacity) {
        int capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
        if (capacity < DEFAULT_CAPACITY) capacity = DEFAULT_CAPACITY;

        this.keys = new int[capacity];
        this.values = new Object[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
        Arrays.fill(keys, EMPTY_KEY);
    }

    private static int hash(int key) {
        key ^= key >>> 16;
        key *= 0x45d9f3b;
        key ^= key >>> 16;
        return key;
    }

    @SuppressWarnings("unchecked")
    public V get(int key) {
        if (key == EMPTY_KEY) key = 0;

        int idx = hash(key) & mask;

        while (keys[idx] != EMPTY_KEY) {
            if (keys[idx] == key) {
                return (V) values[idx];
            }
            idx = (idx + 1) & mask;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public V put(int key, V value) {
        if (key == EMPTY_KEY) key = 0;

        if (size >= threshold) {
            resize();
        }

        int idx = hash(key) & mask;

        while (keys[idx] != EMPTY_KEY) {
            if (keys[idx] == key) {
                V oldValue = (V) values[idx];
                values[idx] = value;
                return oldValue;
            }
            idx = (idx + 1) & mask;
        }

        keys[idx] = key;
        values[idx] = value;
        size++;
        return null;
    }

    @SuppressWarnings("unchecked")
    public V remove(int key) {
        if (key == EMPTY_KEY) key = 0;

        int idx = hash(key) & mask;

        while (keys[idx] != EMPTY_KEY) {
            if (keys[idx] == key) {
                V oldValue = (V) values[idx];

                int nextIdx = (idx + 1) & mask;
                while (keys[nextIdx] != EMPTY_KEY) {
                    int rehashIdx = hash(keys[nextIdx]) & mask;
                    if ((nextIdx > idx && (rehashIdx <= idx || rehashIdx > nextIdx)) ||
                        (nextIdx < idx && (rehashIdx <= idx && rehashIdx > nextIdx))) {
                        keys[idx] = keys[nextIdx];
                        values[idx] = values[nextIdx];
                        idx = nextIdx;
                    }
                    nextIdx = (nextIdx + 1) & mask;
                }

                keys[idx] = EMPTY_KEY;
                values[idx] = null;
                size--;
                return oldValue;
            }
            idx = (idx + 1) & mask;
        }
        return null;
    }

    private void resize() {
        int[] oldKeys = keys;
        Object[] oldValues = values;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new int[newCapacity];
        values = new Object[newCapacity];
        Arrays.fill(keys, EMPTY_KEY);

        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldKeys[i] != EMPTY_KEY) {
                put(oldKeys[i], (V) oldValues[i]);
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
        Arrays.fill(keys, EMPTY_KEY);
        Arrays.fill(values, null);
        size = 0;
    }

}