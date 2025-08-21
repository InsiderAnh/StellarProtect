package io.github.insideranh.stellarprotect.maps;

public class StringBooleanMap {

    private static final int DEFAULT_CAPACITY = 32;
    private static final float LOAD_FACTOR = 0.75f;

    private String[] keys;
    private boolean[] values;
    private byte[] distances;
    private int size;
    private int threshold;
    private int mask;

    public StringBooleanMap() {
        int capacity = DEFAULT_CAPACITY;
        this.keys = new String[capacity];
        this.values = new boolean[capacity];
        this.distances = new byte[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
    }

    private static int hash(String key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    public boolean get(String key) {
        if (key == null) return false;

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
        return false;
    }

    public void put(String key, boolean value) {
        if (key == null) return;
        if (size >= threshold) resize();

        int hash = hash(key);
        int idx = hash & mask;
        String insertKey = key;
        boolean insertValue = value;
        byte insertDistance = 0;

        while (true) {
            if (keys[idx] == null) {
                keys[idx] = insertKey;
                values[idx] = insertValue;
                distances[idx] = insertDistance;
                size++;
                return;
            }

            if (keys[idx].equals(insertKey)) {
                values[idx] = insertValue;
                return;
            }

            if (insertDistance > distances[idx]) {
                String tempKey = keys[idx];
                boolean tempValue = values[idx];
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

    public void remove(String key) {
        if (key == null) return;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (keys[idx].equals(key)) {
                int nextIdx = (idx + 1) & mask;
                while (keys[nextIdx] != null && distances[nextIdx] > 0) {
                    keys[idx] = keys[nextIdx];
                    values[idx] = values[nextIdx];
                    distances[idx] = (byte) (distances[nextIdx] - 1);
                    idx = nextIdx;
                    nextIdx = (nextIdx + 1) & mask;
                }

                keys[idx] = null;
                values[idx] = false;
                distances[idx] = 0;
                size--;
                return;
            }
            idx = (idx + 1) & mask;
            distance++;
        }
    }

    private void resize() {
        String[] oldKeys = keys;
        boolean[] oldValues = values;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new String[newCapacity];
        values = new boolean[newCapacity];
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

}