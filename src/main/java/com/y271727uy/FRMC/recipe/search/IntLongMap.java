/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

public class IntLongMap extends Int2LongOpenHashMap {
    public static final IntLongMap EMPTY = new IntLongMap(0) {
        @Override
        public long get(int key) {
            return 0L;
        }

        @Override
        public boolean containsKey(int key) {
            return false;
        }

        @Override
        public void putAll(IntLongMap map) {
        }

        @Override
        public void copyTo(IntLongMap map) {
        }

        @Override
        public void add(int key, long increment) {
        }

        @Override
        public void copyToArray(int[] keys, long[] values) {
        }

        @Override
        public int[] toIntArray() {
            return new int[0];
        }
    };

    public IntLongMap(int expected) {
        super(expected, 0.75F);
    }

    public IntLongMap() {
        super(16, 0.75F);
    }

    public IntLongMap(IntLongMap map) {
        super(map.size, 0.75F);
        putAll(map);
    }

    @Override
    public long addTo(int key, long increment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long put(int key, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long get(int requestedKey) {
        int[] keys = this.key;
        int position = HashCommon.mix(requestedKey) & this.mask;
        int current = keys[position];
        if (current != 0) {
            do {
                if (current == requestedKey) {
                    return this.value[position];
                }
            } while ((current = keys[position = position + 1 & this.mask]) != 0);
        }
        return 0L;
    }

    @Override
    public boolean containsKey(int requestedKey) {
        int[] keys = this.key;
        int position = HashCommon.mix(requestedKey) & this.mask;
        int current = keys[position];
        if (current != 0) {
            do {
                if (current == requestedKey) {
                    return true;
                }
            } while ((current = keys[position = position + 1 & this.mask]) != 0);
        }
        return false;
    }

    public void add(int requestedKey, long increment) {
        if (requestedKey == 0 || increment == 0L) {
            return;
        }
        int[] keys = this.key;
        int position = HashCommon.mix(requestedKey) & this.mask;
        int current = keys[position];
        if (current != 0) {
            do {
                if (current == requestedKey) {
                    long newValue = this.value[position] + increment;
                    if (newValue < 0L) {
                        newValue = Long.MAX_VALUE;
                    }
                    this.value[position] = newValue;
                    return;
                }
            } while ((current = keys[position = position + 1 & this.mask]) != 0);
        }

        keys[position] = requestedKey;
        this.value[position] = increment;
        if (this.size++ >= this.maxFill) {
            rehash(HashCommon.arraySize(this.size + 1, this.f));
        }
    }

    public void putAll(IntLongMap map) {
        int sourceSize = map.size;
        if (sourceSize == 0) {
            return;
        }
        int[] sourceKeys = map.key;
        long[] sourceValues = map.value;
        int position = map.n;
        int copied = 0;
        while (position-- != 0) {
            int key = sourceKeys[position];
            if (key != 0) {
                add(key, sourceValues[position]);
                if (++copied == sourceSize) {
                    break;
                }
            }
        }
    }

    public void copyTo(IntLongMap map) {
        int sourceSize = this.size;
        if (sourceSize == 0) {
            return;
        }
        int position = this.n;
        int copied = 0;
        while (position-- != 0) {
            int key = this.key[position];
            if (key != 0) {
                map.add(key, this.value[position]);
                if (++copied == sourceSize) {
                    break;
                }
            }
        }
    }

    public void copyToArray(int[] keys, long[] values) {
        int sourceSize = this.size;
        if (sourceSize == 0) {
            return;
        }
        int position = this.n;
        int copied = 0;
        while (position-- != 0) {
            int key = this.key[position];
            if (key != 0) {
                keys[copied] = key;
                values[copied] = this.value[position];
                if (++copied == sourceSize) {
                    break;
                }
            }
        }
    }

    public int[] toIntArray() {
        int sourceSize = this.size;
        int[] result = new int[sourceSize];
        int position = this.n;
        int copied = 0;
        while (position-- != 0) {
            int key = this.key[position];
            if (key != 0) {
                result[copied] = key;
                if (++copied == sourceSize) {
                    break;
                }
            }
        }
        return result;
    }
}
