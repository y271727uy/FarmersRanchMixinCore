/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import java.util.Arrays;

public interface BitContainer {
    BitContainer LONG = new LongImmutable(0L) {
        @Override
        public boolean notContains(int index) {
            return true;
        }
    };

    BitContainer MULTI_LONG = new MultiLongImmutable(new long[0]) {
        @Override
        public boolean notContains(int index) {
            return true;
        }
    };

    default boolean contains(int index) {
        return !notContains(index);
    }

    boolean notContains(int index);

    BitContainer add(int index);

    class LongImmutable implements BitContainer {
        private final long word;

        private LongImmutable(long word) {
            this.word = word;
        }

        @Override
        public boolean notContains(int index) {
            return (word & 1L << index) == 0L;
        }

        @Override
        public BitContainer add(int index) {
            return new LongImmutable(word | 1L << index);
        }
    }

    class MultiLongImmutable implements BitContainer {
        private final long[] words;

        private MultiLongImmutable(long[] words) {
            this.words = words;
        }

        @Override
        public boolean notContains(int index) {
            int wordIndex = index >>> 6;
            if (wordIndex >= words.length) {
                return true;
            }
            int bitIndex = index & 63;
            return (words[wordIndex] & 1L << bitIndex) == 0L;
        }

        @Override
        public BitContainer add(int index) {
            int wordIndex = index >>> 6;
            int requiredWords = wordIndex + 1;
            long[] newWords = requiredWords > words.length
                    ? Arrays.copyOf(words, requiredWords)
                    : words.clone();
            int bitIndex = index & 63;
            newWords[wordIndex] |= 1L << bitIndex;
            return new MultiLongImmutable(newWords);
        }
    }
}
