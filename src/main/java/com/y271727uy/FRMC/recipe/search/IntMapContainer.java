/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

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
