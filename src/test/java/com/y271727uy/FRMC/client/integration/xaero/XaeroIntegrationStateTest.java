package com.y271727uy.FRMC.client.integration.xaero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.integration.xaero.XaeroIntegrationState;
import org.junit.jupiter.api.Test;

class XaeroIntegrationStateTest {
    @Test
    void unavailableIntegrationDoesNotRun() {
        XaeroIntegrationState state = new XaeroIntegrationState();
        state.initialize(false);
        state.runSafely(() -> { throw new AssertionError("must not run"); });
        assertFalse(state.isAvailable());
        assertFalse(state.isEnabled());
        assertFalse(state.hasFailed());
    }

    @Test
    void failedLifecycleIsFusedOff() {
        XaeroIntegrationState state = new XaeroIntegrationState();
        state.initialize(true);
        state.runSafely(() -> { throw new IllegalStateException("test failure"); });
        assertTrue(state.isAvailable());
        assertFalse(state.isEnabled());
        assertTrue(state.hasFailed());
        state.runSafely(() -> { throw new AssertionError("fuse must stay off"); });
    }
}
