package com.y271727uy.FRMC.integration.xaero;

import xaero.map.misc.ConsistentBitArray;

/** Same packed layout as Xaero 1.45.0, with private storage only when needed. */
public final class LazyHeightArray extends ConsistentBitArray {
    private static final int BITS = 13;
    private static final int ENTRIES = 4096;
    private static final long[] EMPTY = new long[(ENTRIES + 64 / BITS - 1) / (64 / BITS)];

    public LazyHeightArray() {
        super(BITS, ENTRIES, EMPTY);
    }

    @Override
    public void set(int index, int value) {
        // Preserve Xaero's own bounds checking even while the array is empty.
        if (super.getData() == EMPTY) {
            int previous = super.get(index);
            if (previous == value) {
                return;
            }
            super.setData(new long[EMPTY.length]);
        }
        super.set(index, value);
    }

    @Override
    public long[] getData() {
        // Xaero exposes its backing array for save/load code. Never let EMPTY escape:
        // callers may mutate the returned array without going through set().
        if (super.getData() == EMPTY) {
            super.setData(new long[EMPTY.length]);
        }
        return super.getData();
    }
}
