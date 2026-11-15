package com.y271727uy.FRMC.integration.embeddium.perf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RadixSortTest {
    /** 参考实现：按键降序的稳定排序，与 Embeddium 的归并排序语义一致。 */
    private static int[] reference(float[] keys) {
        Integer[] boxed = new Integer[keys.length];
        for (int i = 0; i < boxed.length; i++) {
            boxed[i] = i;
        }
        Arrays.sort(boxed, (a, b) -> Float.compare(keys[b], keys[a]));
        return Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
    }

    @Test
    void matchesStableDescendingSortOnRandomKeys() {
        Random random = new Random(42L);
        for (int length : new int[] {0, 1, 2, 255, 256, 257, 4096}) {
            float[] keys = new float[length];
            for (int i = 0; i < length; i++) {
                keys[i] = (random.nextFloat() - 0.5f) * 1000.0f;
            }
            assertArrayEquals(reference(keys), RadixSort.sortDescending(keys), "length " + length);
        }
    }

    @Test
    void keepsOriginalOrderForEqualKeys() {
        Random random = new Random(7L);
        float[] keys = new float[2000];
        for (int i = 0; i < keys.length; i++) {
            // 只有 8 个不同的值，包含负数，大量并列。
            keys[i] = random.nextInt(8) - 4;
        }
        assertArrayEquals(reference(keys), RadixSort.sortDescending(keys));
    }

    @Test
    void handlesIdenticalAndExtremeKeys() {
        float[] same = new float[300];
        Arrays.fill(same, 3.5f);
        assertArrayEquals(reference(same), RadixSort.sortDescending(same));

        float[] extreme = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE, -Float.MIN_VALUE, 1.0f, -1.0f,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        assertArrayEquals(reference(extreme), RadixSort.sortDescending(extreme));
    }
}
