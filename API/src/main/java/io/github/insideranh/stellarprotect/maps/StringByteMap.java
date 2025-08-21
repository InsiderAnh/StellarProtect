package io.github.insideranh.stellarprotect.maps;

public final class StringByteMap {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private String[] keys;
    private byte[] values;
    private byte[] distances;
    private int size;
    private int threshold;
    private int mask;
    private byte defaultReturnValue;

    public StringByteMap(int initialCapacity) {
        int capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
        if (capacity < DEFAULT_CAPACITY) capacity = DEFAULT_CAPACITY;

        this.keys = new String[capacity];
        this.values = new byte[capacity];
        this.distances = new byte[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
        this.defaultReturnValue = 0;
    }

    private static int hash(String key) {
        int h = key.hashCode();
        h ^= h >>> 16;
        return h;
    }

    public void defaultReturnValue(byte defaultValue) {
        this.defaultReturnValue = defaultValue;
    }

    public byte getByte(String key) {
        if (key == null) return defaultReturnValue;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (keys[idx].equals(key)) {
                return values[idx];
            }
            idx = (idx + 1) & mask;
            distance++;
        }
        return defaultReturnValue;
    }

    public byte put(String key, byte value) {
        if (key == null) return defaultReturnValue;

        if (size >= threshold) {
            resize();
        }

        int hash = hash(key);
        int idx = hash & mask;

        String insertKey = key;
        byte insertValue = value;
        byte insertDistance = 0;

        while (true) {
            if (keys[idx] == null) {
                keys[idx] = insertKey;
                values[idx] = insertValue;
                distances[idx] = insertDistance;
                size++;
                return defaultReturnValue;
            }

            if (keys[idx].equals(insertKey)) {
                byte oldValue = values[idx];
                values[idx] = insertValue;
                return oldValue;
            }

            if (insertDistance > distances[idx]) {
                String tempKey = keys[idx];
                byte tempValue = values[idx];
                byte tempDistance = distances[idx];

                keys[idx] = insertKey;
                values[idx] = insertValue;
                distances[idx] = insertDistance;

                insertKey = tempKey;
                insertValue = tempValue;
                insertDistance = tempDistance;
            }

            idx = (idx + 1) & mask;
            insertDistance++;
        }
    }

    private void resize() {
        String[] oldKeys = keys;
        byte[] oldValues = values;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new String[newCapacity];
        values = new byte[newCapacity];
        distances = new byte[newCapacity];

        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldKeys[i] != null) {
                put(oldKeys[i], oldValues[i]);
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
        for (int i = 0; i < keys.length; i++) {
            keys[i] = null;
            values[i] = 0;
            distances[i] = 0;
        }
        size = 0;
    }

}