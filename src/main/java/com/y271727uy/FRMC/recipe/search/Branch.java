/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public interface Branch<R> {
    static <R> Branch<R> create() {
        return new HashBranch<>();
    }

    Node<R> get(int key);

    int size();

    int[] key();

    Node<R>[] value();

    default Branch<R> optimize() {
        return this;
    }

    final class HashBranch<R> extends Int2ObjectOpenHashMap<Node<R>> implements Branch<R> {
        private int[] optimizedKeys;
        private Node<R>[] optimizedValues;

        @Override
        public Node<R> get(int requestedKey) {
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
            return null;
        }

        @Override
        public int[] key() {
            if (optimizedKeys == null) {
                optimizedKeys = keySet().toIntArray();
                this.keys = null;
            }
            return optimizedKeys;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<R>[] value() {
            if (optimizedValues == null) {
                optimizedValues = values().toArray(new Node[0]);
                this.values = null;
            }
            return optimizedValues;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Branch<R> optimize() {
            if (this.size == 0) {
                return null;
            }
            if (this.size < 5) {
                return new ArrayBranch<>(keySet().toIntArray(), values().toArray(new Node[0]));
            }
            return this;
        }

        private static final class ArrayBranch<R> implements Branch<R> {
            private final int[] keys;
            private final Node<R>[] values;

            private ArrayBranch(int[] keys, Node<R>[] values) {
                this.keys = keys;
                this.values = values;
            }

            @Override
            public Node<R> get(int key) {
                for (int i = 0; i < keys.length; i++) {
                    if (key == keys[i]) {
                        return values[i];
                    }
                }
                return null;
            }

            @Override
            public int size() {
                return keys.length;
            }

            @Override
            public int[] key() {
                return keys;
            }

            @Override
            public Node<R>[] value() {
                return values;
            }
        }
    }
}
