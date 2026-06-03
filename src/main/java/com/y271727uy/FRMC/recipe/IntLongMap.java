package com.y271727uy.FRMC.recipe;

import java.util.Arrays;

/**
 * A specialized map from int keys to long values, optimized for the recipe search use case.
 * Internally uses two parallel arrays that grow as needed.
 */
public class IntLongMap {
    private static final int INITIAL_CAPACITY = 8;

    private int[] keys;
    private long[] values;
    private int size;

    public IntLongMap() {
        this.keys = new int[INITIAL_CAPACITY];
        this.values = new long[INITIAL_CAPACITY];
        this.size = 0;
    }

    public void add(int key, long increment) {
        int index = findKey(key);
        if (index >= 0) {
            values[index] += increment;
        } else {
            ensureCapacity();
            keys[size] = key;
            values[size] = increment;
            size++;
        }
    }

    public long get(int key) {
        int index = findKey(key);
        return index >= 0 ? values[index] : 0;
    }

    public boolean containsKey(int key) {
        return findKey(key) >= 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public int[] toIntArray() {
        return Arrays.copyOf(keys, size);
    }

    public boolean containsAll(IntLongMap other) {
        for (int i = 0; i < other.size; i++) {
            int key = other.keys[i];
            long needed = other.values[i];
            if (get(key) < needed) {
                return false;
            }
        }
        return true;
    }

    private int findKey(int key) {
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                return i;
            }
        }
        return -1;
    }

    private void ensureCapacity() {
        if (size >= keys.length) {
            int newCapacity = keys.length * 2;
            keys = Arrays.copyOf(keys, newCapacity);
            values = Arrays.copyOf(values, newCapacity);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(keys[i]).append("=").append(values[i]);
        }
        sb.append("}");
        return sb.toString();
    }
}
