package com.y271727uy.FRMC.capability.blockchecking;

import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.runtime.BlockCheckingSession;
import com.y271727uy.FRMC.capability.blockchecking.runtime.WorkContextTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockCheckingRuntimeTest {
    @Test
    void sessionAllowsOneActiveAndRetainsReport() {
        BlockCheckingSession session = new BlockCheckingSession();
        assertTrue(session.start(0, 1));
        assertFalse(session.start(0, 1));
        assertTrue(session.stop(10, 4));
        assertNotNull(session.lastReport());
        assertFalse(session.active());
        session.clear();
        assertNull(session.lastReport());
    }

    @Test
    void contextValidatesRevisionAndDepth() {
        WorkContextTracker tracker = new WorkContextTracker();
        long outer = tracker.enter(WorkKind.GAME_LOGIC, 10);
        long inner = tracker.enter(WorkKind.ENTITY_AI, 20);
        assertFalse(tracker.exit(outer));
        assertEquals(2, tracker.capture(50).depth());
        assertTrue(tracker.exit(inner));
        assertTrue(tracker.exit(outer));
        assertEquals(0, tracker.depth());
    }
}
