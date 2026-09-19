package com.y271727uy.FRMC.capability.recipe.search;

public class IntMapContainer {
    final int[] key;
    long[] value;

    public IntMapContainer(int[] key) {
        this.key = key;
    }

    public boolean match(IntLongMap map) {
        for (int i = key.length - 1; i >= 0; i--) {
            if (map.get(key[i]) < value[i]) {
                return false;
            }
        }
        return true;
    }
}
