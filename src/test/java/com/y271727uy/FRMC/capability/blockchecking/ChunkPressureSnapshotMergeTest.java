package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureEntry;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkPressureSnapshotMergeTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void emptyBaseBecomesSingleRow() {
        ChunkPressureEntry inspected = new ChunkPressureEntry(OVERWORLD, 1L, 0, 0, 0, 0);
        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(null, inspected, 42L);

        assertEquals(42L, merged.capturedAtMillis());
        assertEquals(1, merged.entries().size());
        assertSame(inspected, merged.entries().get(0));
        assertTrue(merged.trackedMobCoverage());
    }

    @Test
    void emptySnapshotBaseBecomesSingleRow() {
        ChunkPressureEntry inspected = new ChunkPressureEntry(OVERWORLD, 7L, 0, 0, 0, 4);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(), true);
        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(base, inspected, 9L);

        assertEquals(List.of(inspected), merged.entries());
        assertEquals(9L, merged.capturedAtMillis());
    }

    @Test
    void replacesSameDimensionAndPackedChunk() {
        ChunkPressureEntry oldRow = new ChunkPressureEntry(OVERWORLD, 1L, 8, 0, 5, 0);
        ChunkPressureEntry other = new ChunkPressureEntry(OVERWORLD, 2L, 1, 0, 0, 0);
        ChunkPressureEntry nether = new ChunkPressureEntry(NETHER, 1L, 3, 0, 1, 0);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(oldRow, other, nether), true);
        ChunkPressureEntry inspected = new ChunkPressureEntry(OVERWORLD, 1L, 0, 0, 0, 12);

        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(base, inspected, 5L);

        assertEquals(3, merged.entries().size());
        assertSame(inspected, merged.entries().get(0));
        assertSame(other, merged.entries().get(1));
        assertSame(nether, merged.entries().get(2));
    }

    @Test
    void prependsUnknownChunkAndKeepsOthers() {
        ChunkPressureEntry first = new ChunkPressureEntry(OVERWORLD, 10L, 2, 0, 0, 0);
        ChunkPressureEntry second = new ChunkPressureEntry(OVERWORLD, 11L, 4, 1, 0, 0);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(first, second), false);
        ChunkPressureEntry inspected = new ChunkPressureEntry(OVERWORLD, 12L, 0, 0, 0, 0);

        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(base, inspected, 8L);

        assertEquals(List.of(inspected, first, second), merged.entries());
        assertFalse(merged.trackedMobCoverage());
        assertEquals(8L, merged.capturedAtMillis());
    }

    @Test
    void limitKeepsInspectedRowFirst() {
        ChunkPressureEntry a = new ChunkPressureEntry(OVERWORLD, 1L, 1, 0, 0, 0);
        ChunkPressureEntry b = new ChunkPressureEntry(OVERWORLD, 2L, 2, 0, 0, 0);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(a, b), true);
        ChunkPressureEntry inspected = new ChunkPressureEntry(OVERWORLD, 3L, 0, 0, 0, 0);

        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(base, inspected, 3L, 2);

        assertEquals(List.of(inspected, a), merged.entries());
    }

    @Test
    void mergedEntriesAreImmutable() {
        ChunkPressureSnapshot merged = ChunkPressureSnapshot.merge(null,
                new ChunkPressureEntry(OVERWORLD, 1L, 0, 0, 0, 0), 1L);
        assertThrows(UnsupportedOperationException.class, () -> merged.entries().add(null));
    }

    @Test
    void withoutDropsMatchingRowAndKeepsCoverage() {
        ChunkPressureEntry keep = new ChunkPressureEntry(OVERWORLD, 2L, 1, 0, 0, 0);
        ChunkPressureEntry drop = new ChunkPressureEntry(OVERWORLD, 1L, 8, 0, 5, 0);
        ChunkPressureEntry nether = new ChunkPressureEntry(NETHER, 1L, 3, 0, 1, 0);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(drop, keep, nether), false);

        ChunkPressureSnapshot next = ChunkPressureSnapshot.without(base, OVERWORLD, 1L, 9L);

        assertEquals(9L, next.capturedAtMillis());
        assertEquals(List.of(keep, nether), next.entries());
        assertFalse(next.trackedMobCoverage());
    }

    @Test
    void withoutUnknownOrBlankDimensionKeepsRows() {
        ChunkPressureEntry row = new ChunkPressureEntry(OVERWORLD, 1L, 1, 0, 0, 0);
        ChunkPressureSnapshot base = new ChunkPressureSnapshot(1L, List.of(row), true);

        assertEquals(List.of(row), ChunkPressureSnapshot.without(base, OVERWORLD, 99L, 2L).entries());
        assertTrue(ChunkPressureSnapshot.without(null, OVERWORLD, 1L, 3L).entries().isEmpty());
        assertEquals(List.of(row), ChunkPressureSnapshot.without(base, "", 1L, 4L).entries());
        assertEquals(List.of(row), ChunkPressureSnapshot.without(base, null, 1L, 5L).entries());
    }
}
