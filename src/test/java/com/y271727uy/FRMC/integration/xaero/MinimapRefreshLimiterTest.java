package com.y271727uy.FRMC.integration.xaero;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MinimapRefreshLimiterTest {
    @Test
    void firstFrameAndInvalidationAlwaysRefresh() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        assertTrue(limiter.shouldRefresh(0L, 60));
        limiter.rendered(0L);
        assertFalse(limiter.shouldRefresh(1L, 60));
        limiter.invalidate();
        assertTrue(limiter.shouldRefresh(1L, 60));
    }

    @Test
    void boundsOffscreenRedrawsAtSixHundredGameFramesPerSecond() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        int refreshed = 0;
        for (int frame = 0; frame < 600; frame++) {
            long now = frame * 1_000_000_000L / 600;
            if (limiter.shouldRefresh(now, 60)) {
                refreshed++;
                limiter.rendered(now);
            }
        }
        assertEquals(60, refreshed);
    }

    @Test
    void doesNotDropFramesWhenGameIsSlowerThanTheCap() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        for (int frame = 0; frame < 30; frame++) {
            long now = frame * 1_000_000_000L / 30;
            assertTrue(limiter.shouldRefresh(now, 60));
            limiter.rendered(now);
        }
    }

    @Test
    void disabledCapAndChangedCapTakeEffectImmediately() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        limiter.rendered(0L);
        assertTrue(limiter.shouldRefresh(0L, 0));
        assertFalse(limiter.shouldRefresh(10_000_000L, 60));
        assertTrue(limiter.shouldRefresh(10_000_000L, 120));
    }

    @Test
    void usesNanosecondsRatherThanRoundedMilliseconds() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        limiter.rendered(0L);
        assertFalse(limiter.shouldRefresh(16_000_000L, 60));
        assertTrue(limiter.shouldRefresh(16_666_666L, 60));
    }

    @Test
    void decisionDoesNotReserveAFrameBeforeSuccessfulDrawing() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        limiter.rendered(0L);
        assertTrue(limiter.shouldRefresh(20_000_000L, 60));
        assertTrue(limiter.shouldRefresh(20_000_001L, 60));
        limiter.rendered(20_000_001L);
        assertFalse(limiter.shouldRefresh(20_000_002L, 60));
    }

    @Test
    void clockResetAndNanoTimeWrapDoNotFreezeMap() {
        MinimapRefreshLimiter limiter = new MinimapRefreshLimiter();
        limiter.rendered(100_000_000L);
        assertTrue(limiter.shouldRefresh(1L, 60));
        limiter.rendered(Long.MAX_VALUE - 5_000_000L);
        assertFalse(limiter.shouldRefresh(Long.MIN_VALUE + 5_000_000L, 60));
        assertTrue(limiter.shouldRefresh(Long.MIN_VALUE + 20_000_000L, 60));
    }
}
