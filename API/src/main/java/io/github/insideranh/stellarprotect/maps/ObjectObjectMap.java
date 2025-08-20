package io.github.insideranh.stellarprotect.maps;

import java.util.Collection;
import java.util.Objects;

public final class ObjectObjectMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    private Object[] keys;
    private Object[] values;
    private byte[] distances;
    private int size;
    private int threshold;
    private int mask;

    public ObjectObjectMap(int initialCapacity) {
        int capacity = Integer.highestOneBit(initialCapacity - 1) << 1;
        if (capacity < DEFAULT_CAPACITY) capacity = DEFAULT_CAPACITY;

        this.keys = new Object[capacity];
        this.values = new Object[capacity];
        this.distances = new byte[capacity];
        this.mask = capacity - 1;
        this.threshold = (int) (capacity * LOAD_FACTOR);
    }

    public ObjectObjectMap() {
        this(DEFAULT_CAPACITY);
    }

    private static int hash(Object key) {
        int h = key.hashCode();
        h ^= h >>> 16;
        return h;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (key == null) return null;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (Objects.equals(keys[idx], key)) {
                return (V) values[idx];
            }
            idx = (idx + 1) & mask;
            distance++;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key == null) return null;

        if (size >= threshold) {
            resize();
        }

        int hash = hash(key);
        int idx = hash & mask;

        Object insertKey = key;
        Object insertValue = value;
        byte insertDistance = 0;

        while (true) {
            if (keys[idx] == null) {
                keys[idx] = insertKey;
                values[idx] = insertValue;
                distances[idx] = insertDistance;
                size++;
                return null;
            }

            if (Objects.equals(keys[idx], insertKey)) {
                V oldValue = (V) values[idx];
                values[idx] = insertValue;
                return oldValue;
            }

            if (insertDistance > distances[idx]) {
                Object tempKey = keys[idx];
                Object tempValue = values[idx];
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

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (key == null) return null;

        int hash = hash(key);
        int idx = hash & mask;
        byte distance = 0;

        while (keys[idx] != null && distance <= distances[idx]) {
            if (Objects.equals(keys[idx], key)) {
                V oldValue = (V) values[idx];

                int nextIdx = (idx + 1) & mask;
                while (keys[nextIdx] != null && distances[nextIdx] > 0) {
                    keys[idx] = keys[nextIdx];
                    values[idx] = values[nextIdx];
                    distances[idx] = (byte) (distances[nextIdx] - 1);
                    idx = nextIdx;
                    nextIdx = (nextIdx + 1) & mask;
                }

                keys[idx] = null;
                values[idx] = null;
                distances[idx] = 0;
                size--;
                return oldValue;
            }
            idx = (idx + 1) & mask;
            distance++;
        }
        return null;
    }

    public Collection<V> values() {
        return new java.util.AbstractCollection<V>() {
            public java.util.Iterator<V> iterator() {
                return new java.util.Iterator<V>() {
                    private int index = 0;
                    private int returned = 0;

                    public boolean hasNext() {
                        return returned < size;
                    }

                    @SuppressWarnings("unchecked")
                    public V next() {
                        while (keys[index] == null) index++;
                        returned++;
                        return (V) values[index++];
                    }
                };
            }

            public int size() {
                return ObjectObjectMap.this.size;
            }
        };
    }

    private void resize() {
        Object[] oldKeys = keys;
        Object[] oldValues = values;
        int oldCapacity = keys.length;

        int newCapacity = oldCapacity << 1;
        keys = new Object[newCapacity];
        values = new Object[newCapacity];
        distances = new byte[newCapacity];

        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldKeys[i] != null) {
                put((K) oldKeys[i], (V) oldValues[i]);
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

}