package io.github.insideranh.stellarprotect.cache;

import io.github.insideranh.stellarprotect.items.ItemTemplate;

import java.util.*;

public class ItemsCache {

    private static final int CAPACITY = 8192;
    private static final int MASK = CAPACITY - 1;
    private static final float LOAD_FACTOR = 0.7f;

    private final ItemTemplate[] items = new ItemTemplate[CAPACITY];

    private final IndexEntry[] idIndex = new IndexEntry[CAPACITY];
    private final IndexEntry[] displayNameIndex = new IndexEntry[CAPACITY];
    private final IndexEntry[] loreIndex = new IndexEntry[CAPACITY];
    private final IndexEntry[] typeNameIndex = new IndexEntry[CAPACITY];

    private final Map<String, IntOpenHashSet> displayNameTokens = new HashMap<>();
    private final Map<String, IntOpenHashSet> loreTokens = new HashMap<>();
    private final Map<String, IntOpenHashSet> typeNameTokens = new HashMap<>();

    private final int[] validPositions = new int[CAPACITY];
    private int validCount = 0;

    private int size = 0;

    private static class IntOpenHashSet {

        private int[] keys;
        private boolean[] allocated;
        private int size;

        public IntOpenHashSet() {
            this(16);
        }

        public IntOpenHashSet(int capacity) {
            capacity = nextPowerOfTwo(capacity);
            keys = new int[capacity];
            allocated = new boolean[capacity];
            size = 0;
        }

        private static int nextPowerOfTwo(int n) {
            n--;
            n |= n >> 1;
            n |= n >> 2;
            n |= n >> 4;
            n |= n >> 8;
            n |= n >> 16;
            return n + 1;
        }

        public void add(int key) {
            float loadFactor = 0.75f;
            if (size >= keys.length * loadFactor) {
                resize();
            }

            int slot = key & (keys.length - 1);
            while (allocated[slot]) {
                if (keys[slot] == key) return;
                slot = (slot + 1) & (keys.length - 1);
            }

            keys[slot] = key;
            allocated[slot] = true;
            size++;
        }

        public int[] toArray() {
            int[] result = new int[size];
            int idx = 0;
            for (int i = 0; i < allocated.length; i++) {
                if (allocated[i]) {
                    result[idx++] = keys[i];
                }
            }
            return result;
        }

        private void resize() {

            int[] oldKeys = keys;
            boolean[] oldAllocated = allocated;

            keys = new int[oldKeys.length * 2];
            allocated = new boolean[oldAllocated.length * 2];
            size = 0;

            for (int i = 0; i < oldAllocated.length; i++) {
                if (oldAllocated[i]) {
                    add(oldKeys[i]);
                }
            }

        }

    }

    private static class IndexEntry {

        final long hash;
        final int position;
        IndexEntry next;

        IndexEntry(long hash, int position) {
            this.hash = hash;
            this.position = position;
        }

    }

    private static long fastHash(String str) {
        if (str == null) return 0;

        long hash = 0;
        final int len = str.length();

        int i = 0;
        for (; i < len - 3; i += 4) {
            hash = (hash << 5) - hash + str.charAt(i);
            hash = (hash << 5) - hash + str.charAt(i + 1);
            hash = (hash << 5) - hash + str.charAt(i + 2);
            hash = (hash << 5) - hash + str.charAt(i + 3);
        }

        for (; i < len; i++) {
            hash = (hash << 5) - hash + str.charAt(i);
        }

        return hash;
    }

    private static long fastHashLong(long value) {
        value = (~value) + (value << 18);
        value = value ^ (value >>> 31);
        value = value * 21;
        value = value ^ (value >>> 11);
        value = value + (value << 6);
        value = value ^ (value >>> 22);
        return value;
    }

    private void indexSubstrings(String text, Map<String, IntOpenHashSet> tokenMap, int position) {
        if (text == null || text.isEmpty()) return;

        String lowerText = text.toLowerCase();

        for (int len = 2; len <= Math.min(6, lowerText.length()); len++) {
            for (int i = 0; i <= lowerText.length() - len; i++) {
                String substring = lowerText.substring(i, i + len);

                tokenMap.computeIfAbsent(substring, k -> new IntOpenHashSet()).add(position);
            }
        }

        String[] words = lowerText.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                tokenMap.computeIfAbsent(word, k -> new IntOpenHashSet()).add(position);
            }
        }
    }

    private int findFreePosition() {
        for (int i = 0; i < CAPACITY; i++) {
            if (items[i] == null) {
                return i;
            }
        }
        throw new IllegalStateException("Cache full");
    }

    private void addToStringIndex(IndexEntry[] index, String key, int position) {
        if (key == null) return;

        long hash = fastHash(key);
        int slot = (int)(hash & MASK);

        IndexEntry entry = new IndexEntry(hash, position);

        while (index[slot] != null) {
            if (index[slot].hash == hash) {
                entry.next = index[slot].next;
                index[slot] = entry;
                return;
            }
            slot = (slot + 1) & MASK;
        }

        index[slot] = entry;
    }

    private void addToIdIndex(long id, int position) {
        long hash = fastHashLong(id);
        int slot = (int)(hash & MASK);

        IndexEntry entry = new IndexEntry(hash, position);

        while (idIndex[slot] != null) {
            if (idIndex[slot].hash == hash) {
                entry.next = idIndex[slot].next;
                idIndex[slot] = entry;
                return;
            }
            slot = (slot + 1) & MASK;
        }

        idIndex[slot] = entry;
    }

    private ItemTemplate searchInStringIndex(IndexEntry[] index, String key) {
        if (key == null) return null;

        long hash = fastHash(key);
        int slot = (int)(hash & MASK);

        while (index[slot] != null) {
            IndexEntry entry = index[slot];

            if (entry.hash == hash) {
                ItemTemplate item = items[entry.position];

                if (item != null &&
                    ((index == typeNameIndex && key.equals(item.getTypeName()) ||
                        (index == loreIndex && key.equals(item.getLore())) ||
                        (index == displayNameIndex && key.equals(item.getDisplayName()))))) {
                    return item;
                }
            }

            slot = (slot + 1) & MASK;
        }

        return null;
    }

    private ItemTemplate searchById(long id) {
        long hash = fastHashLong(id);
        int slot = (int)(hash & MASK);

        while (idIndex[slot] != null) {
            IndexEntry entry = idIndex[slot];

            if (entry.hash == hash) {
                ItemTemplate item = items[entry.position];

                if (item != null && item.id == id) {
                    return item;
                }
            }

            slot = (slot + 1) & MASK;
        }

        return null;
    }

    private List<Long> findContains(String searchText, Map<String, IntOpenHashSet> tokenMap, FieldType fieldType) {
        if (searchText == null || searchText.isEmpty()) {
            return new ArrayList<>();
        }

        String lowerSearch = searchText.toLowerCase();
        List<Long> results = new ArrayList<>();

        IntOpenHashSet candidatePositions = tokenMap.get(lowerSearch);
        if (candidatePositions != null) {
            int[] positions = candidatePositions.toArray();
            for (int pos : positions) {
                ItemTemplate item = items[pos];
                if (item != null) {
                    results.add(item.id);
                }
            }
            return results;
        }

        String bestMatch = null;
        IntOpenHashSet bestPositions = null;

        for (int len = Math.min(lowerSearch.length(), 6); len >= 2; len--) {
            for (int i = 0; i <= lowerSearch.length() - len; i++) {
                String candidate = lowerSearch.substring(i, i + len);
                IntOpenHashSet positions = tokenMap.get(candidate);
                if (positions != null) {
                    bestMatch = candidate;
                    bestPositions = positions;
                    break;
                }
            }
            if (bestMatch != null) break;
        }

        if (bestPositions != null) {
            int[] positions = bestPositions.toArray();
            for (int pos : positions) {
                ItemTemplate item = items[pos];
                if (item != null) {
                    String fieldValue = getFieldValue(item, fieldType);
                    if (fieldValue != null && fieldValue.contains(lowerSearch)) {
                        results.add(item.id);
                    }
                }
            }
            return results;
        }

        for (int i = 0; i < validCount; i++) {
            int pos = validPositions[i];
            ItemTemplate item = items[pos];
            if (item != null) {
                String fieldValue = getFieldValue(item, fieldType);
                if (fieldValue != null && fieldValue.contains(lowerSearch)) {
                    results.add(item.id);
                }
            }
        }

        return results;
    }

    private enum FieldType { DISPLAY_NAME, LORE, TYPE_NAME, LOWER_DISPLAY_NAME, LOWER_LORE, LOWER_TYPE_NAME }

    private String getFieldValue(ItemTemplate item, FieldType fieldType) {
        switch (fieldType) {
            case DISPLAY_NAME: return item.getDisplayName();
            case LORE: return item.getLore();
            case TYPE_NAME: return item.getTypeName();
            case LOWER_DISPLAY_NAME: return item.getDisplayNameLower();
            case LOWER_LORE: return item.getLoreLower();
            case LOWER_TYPE_NAME: return item.getTypeNameLower();
            default: return null;
        }
    }

    public boolean put(ItemTemplate item) {
        if (size >= CAPACITY * LOAD_FACTOR) {
            return false;
        }

        int position = findFreePosition();

        items[position] = item;

        validPositions[validCount++] = position;

        addToIdIndex(item.id, position);
        if (item.getDisplayName() != null) addToStringIndex(displayNameIndex, item.getDisplayName(), position);
        if (item.getLore() != null) addToStringIndex(loreIndex, item.getLore(), position);
        if (item.getTypeName() != null) addToStringIndex(typeNameIndex, item.getTypeName(), position);

        if (item.getDisplayName() != null) indexSubstrings(item.getDisplayName(), displayNameTokens, position);
        if (item.getLore() != null) indexSubstrings(item.getLore(), loreTokens, position);
        if (item.getTypeName() != null) indexSubstrings(item.getTypeName(), typeNameTokens, position);

        size++;
        return true;
    }

    public ItemTemplate getById(long id) {
        return searchById(id);
    }

    public ItemTemplate getByDisplayNameExact(String displayName) {
        return searchInStringIndex(displayNameIndex, displayName);
    }

    public ItemTemplate getByLoreExact(String lore) {
        return searchInStringIndex(loreIndex, lore);
    }

    public ItemTemplate getByTypeNameExact(String typeName) {
        return searchInStringIndex(typeNameIndex, typeName);
    }

    public List<Long> findIdsByDisplayNameContains(String searchText) {
        return findContains(searchText, displayNameTokens, FieldType.LOWER_DISPLAY_NAME);
    }

    public List<Long> findIdsByLoreContains(String searchText) {
        return findContains(searchText, loreTokens, FieldType.LOWER_LORE);
    }

    public List<Long> findIdsByTypeNameContains(String searchText) {
        return findContains(searchText, typeNameTokens, FieldType.LOWER_TYPE_NAME);
    }

    public int size() { return size; }

}