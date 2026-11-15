package com.y271727uy.FRMC.integration.xaero;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Random;
import org.junit.jupiter.api.Test;
import xaero.map.misc.ConsistentBitArray;

class LazyHeightArrayTest {
    @Test
    void zeroReadsAndZeroWritesKeepSharedStorage() throws Exception {
        LazyHeightArray first = new LazyHeightArray();
        LazyHeightArray second = new LazyHeightArray();
        long[] shared = storage(first);
        assertSame(shared, storage(second));
        for (int index = 0; index < 4096; index++) {
            assertEquals(0, first.get(index));
            first.set(index, 0);
        }
        assertSame(shared, storage(first));
    }

    @Test
    void firstWriteDetachesOnlyTheWrittenArray() throws Exception {
        LazyHeightArray first = new LazyHeightArray();
        LazyHeightArray second = new LazyHeightArray();
        long[] shared = storage(first);
        first.set(4095, 8191);
        assertEquals(8191, first.get(4095));
        assertEquals(0, second.get(4095));
        assertNotSame(shared, storage(first));
        assertSame(shared, storage(second));
    }

    @Test
    void rawBackingArrayCannotModifyOtherEmptyTiles() {
        LazyHeightArray first = new LazyHeightArray();
        LazyHeightArray second = new LazyHeightArray();
        first.getData()[0] = -1L;
        assertEquals(8191, first.get(0));
        assertEquals(0, second.get(0));
    }

    @Test
    void preservesXaeroPackedValuesAndSerializationAcrossRandomWrites() {
        LazyHeightArray actual = new LazyHeightArray();
        ConsistentBitArray expected = new ConsistentBitArray(13, 4096);
        Random random = new Random(82467L);
        for (int i = 0; i < 20000; i++) {
            int index = random.nextInt(4096);
            int value = random.nextInt(16384); // Also exercise original masking of oversized values.
            actual.set(index, value);
            expected.set(index, value);
            assertEquals(expected.get(index), actual.get(index));
        }
        assertArrayEquals(expected.getData(), actual.getData());
        actual.setData(expected.getData().clone());
        for (int index = 0; index < 4096; index++) {
            assertEquals(expected.get(index), actual.get(index));
        }
    }

    @Test
    void preservesOutOfBoundsWriteFailure() throws Exception {
        LazyHeightArray array = new LazyHeightArray();
        long[] shared = storage(array);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.set(4096, 1));
        assertSame(shared, storage(array));
    }

    private static long[] storage(ConsistentBitArray array) throws Exception {
        Field field = ConsistentBitArray.class.getDeclaredField("data");
        field.setAccessible(true);
        return (long[]) field.get(array);
    }
}
