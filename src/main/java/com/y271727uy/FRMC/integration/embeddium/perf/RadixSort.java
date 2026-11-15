package com.y271727uy.FRMC.integration.embeddium.perf;

/**
 * 按 float 键降序排列下标的 LSD 基数排序，稳定（键相等时保持原下标顺序）。
 * 移植自 Emberbium（Embeddium 的 fork，LGPL-3.0）的 {@code me.jellysquid.mods.sodium.client.util.sorting.RadixSort}。
 */
public final class RadixSort {
    /** 小于这个长度时归并排序更快，由调用方回退。 */
    public static final int THRESHOLD = 256;

    private static final int RADIX_BITS = 8;
    private static final int BUCKETS = 1 << RADIX_BITS;
    private static final int BUCKET_MASK = BUCKETS - 1;
    private static final int PASSES = Integer.SIZE / RADIX_BITS;

    private RadixSort() {
    }

    public static int[] sortDescending(float[] keys) {
        final int length = keys.length;
        final int[] sortable = new int[length];
        final int[] counts = new int[PASSES * BUCKETS];

        for (int index = 0; index < length; index++) {
            int bits = Float.floatToRawIntBits(keys[index]);
            // 先把 IEEE 754 位模式变成可按无符号整数比较的升序键，再取反得到降序。
            int key = ~(bits ^ ((bits >> 31) | Integer.MIN_VALUE));
            sortable[index] = key;

            for (int pass = 0; pass < PASSES; pass++) {
                counts[(pass * BUCKETS) + ((key >>> (pass * RADIX_BITS)) & BUCKET_MASK)]++;
            }
        }

        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        if (length == 0) {
            return indices;
        }

        int[] scratch = new int[length];

        for (int pass = 0; pass < PASSES; pass++) {
            final int offset = pass * BUCKETS;
            final int shift = pass * RADIX_BITS;

            // 所有键在这一字节上相同，这一趟不会改变顺序。
            if (counts[offset + ((sortable[0] >>> shift) & BUCKET_MASK)] == length) {
                continue;
            }

            int sum = 0;

            for (int bucket = 0; bucket < BUCKETS; bucket++) {
                int count = counts[offset + bucket];
                counts[offset + bucket] = sum;
                sum += count;
            }

            for (int i = 0; i < length; i++) {
                int index = indices[i];
                scratch[counts[offset + ((sortable[index] >>> shift) & BUCKET_MASK)]++] = index;
            }

            int[] swap = indices;
            indices = scratch;
            scratch = swap;
        }

        return indices;
    }
}
