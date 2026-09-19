package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.capability.blockchecking.client.ClientBlockCheckingState;
import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientBlockCheckingStateTest {
    @Test
    void rejectsOlderRevisionAndTimestamp() {
        ClientBlockCheckingState state = new ClientBlockCheckingState();
        ChunkPressureSnapshot first = new ChunkPressureSnapshot(10L, List.of(), true);
        ChunkPressureSnapshot second = new ChunkPressureSnapshot(20L, List.of(), false);
        assertTrue(state.updateSnapshot(2L, 20L, first));
        assertFalse(state.updateSnapshot(1L, 30L, second));
        assertFalse(state.updateSnapshot(2L, 20L, second));
        assertSame(first, state.snapshot());
        assertTrue(state.updateSnapshot(2L, 21L, second));
        assertSame(second, state.snapshot());
    }

    @Test
    void snapshotEntriesAreImmutable() {
        ChunkPressureSnapshot snapshot = new ChunkPressureSnapshot(1L, List.of(), true);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.entries().add(null));
    }

    @Test
    void invalidateSnapshotKeepsReportAndAcceptsNextPacket() {
        ClientBlockCheckingState state = new ClientBlockCheckingState();
        ChunkPressureSnapshot first = new ChunkPressureSnapshot(1L, List.of(), true);
        state.updateSnapshot(1L, 1L, first);
        state.invalidateSnapshot();
        assertNull(state.snapshot());
        assertEquals(-1L, state.snapshotRevision());
        ChunkPressureSnapshot second = new ChunkPressureSnapshot(2L, List.of(), false);
        assertTrue(state.updateSnapshot(1L, 1L, second));
        assertSame(second, state.snapshot());
    }

    @Test
    void clearDropsBothViews() {
        ClientBlockCheckingState state = new ClientBlockCheckingState();
        state.updateSnapshot(1L, 1L, new ChunkPressureSnapshot(1L, List.of(), true));
        state.clear();
        assertNull(state.snapshot());
        assertNull(state.report());
        assertEquals(-1L, state.snapshotRevision());
        assertEquals(-1L, state.reportRevision());
    }
}
