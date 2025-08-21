package io.github.insideranh.stellarprotect.maps;

public final class StringHashSet {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private String[] keys;
    private byte[] distances;
    private int size;
    private int threshold;
    private int mask;

    public StringHashSet(int initialCapacity) {
        int capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
        if (capacity < DEFAULT_CAPACITY) capacity = DEFAULT_CAPACITY;

        this.keys = new String[capacity];
        this.distances = new byte[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
    }

    private static int hash(String key) {
        int h = key.hashCode();
        h ^= h >>> 16;
        return h;
    }

    public boolean add(String key) {
        if (key == null) return false;

        if (size >= threshold) {
            resize();
        }

        int hash = hash(key);
        int idx = hash & mask;

        String insertKey = key;
        byte insertDistance = 0;

        while (true) {
            if (keys[idx] == null) {
                keys[idx] = insertKey;
                distances[idx] = insertDistance;
                size++;
                return true;
            }

            if (keys[idx].equals(insertKey)) {
                return false;
            }

            if (insertDistance > distances[idx]) {
                String tempKey = keys[idx];
                byte tempDistance = distances[idx];

                keys[idx] = insertKey;
                distances[idx] = insertDistance;

                insertKey = tempKey;
                insertDistance = tempDistance;
            }

            idx = (idx + 1) & mask;
            insertDistance++;
        }
    }

    public boolean contains(String key) {
        if (key == null) return false;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (keys[idx].equals(key)) {
                return true;
            }
            idx = (idx + 1) & mask;
            distance++;
        }
        return false;
    }

    public boolean remove(String key) {
        if (key == null) return false;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (keys[idx].equals(key)) {
                int nextIdx = (idx + 1) & mask;
                while (keys[nextIdx] != null && distances[nextIdx] > 0) {
                    keys[idx] = keys[nextIdx];
                    distances[idx] = (byte) (distances[nextIdx] - 1);
                    idx = nextIdx;
                    nextIdx = (nextIdx + 1) & mask;
                }

                keys[idx] = null;
                distances[idx] = 0;
                size--;
                return true;
            }
            idx = (idx + 1) & mask;
            distance++;
        }
        return false;
    }

    private void resize() {
        String[] oldKeys = keys;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new String[newCapacity];
        distances = new byte[newCapacity];

        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldKeys[i] != null) {
                add(oldKeys[i]);
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
            distances[i] = 0;
        }
        size = 0;
    }

    public void addAll(String[] elements) {
        for (String element : elements) {
            add(element);
        }
    }

    public void addAll(java.util.Collection<String> elements) {
        for (String element : elements) {
            add(element);
        }
    }

    public void addAll(StringHashSet other) {
        for (int i = 0; i < other.keys.length; i++) {
            if (other.keys[i] != null) {
                add(other.keys[i]);
            }
        }
    }

}