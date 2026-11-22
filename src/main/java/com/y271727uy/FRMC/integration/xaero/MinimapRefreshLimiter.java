package com.y271727uy.FRMC.integration.xaero;

/** Minimum spacing between successful off-screen renders; owns no game objects. */
public final class MinimapRefreshLimiter {
    private boolean hasFrame;
    private long lastRendered;

    public boolean shouldRefresh(long nowNanos, int maxRefreshesPerSecond) {
        if (!hasFrame || maxRefreshesPerSecond <= 0) {
            return true;
        }
        long elapsed = nowNanos - lastRendered;
        // A reset clock must never leave the map frozen. Subtraction also handles
        // nanoTime wrapping as long as the measured interval is below 2^63 ns.
        return elapsed < 0L || elapsed >= 1_000_000_000L / maxRefreshesPerSecond;
    }

    /** Called only after the original renderer returns successfully. */
    public void rendered(long startedAtNanos) {
        lastRendered = startedAtNanos;
        hasFrame = true;
    }

    public void invalidate() {
        hasFrame = false;
    }
}
