package com.y271727uy.FRMC.capability.blockchecking.runtime;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.ReportAggregator;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkObservation;

/** Thread-safe lifecycle for the sole active block-checking session. */
public final class BlockCheckingSession {
    private boolean active;
    /**
     * Lock-free mirror of {@link #active}, written only while holding the monitor.
     * Hot tick paths poll {@link #active()} for every entity/block entity/chunk; since JDK 15 removed
     * biased locking, a synchronized getter there costs an atomic CAS per call even when idle.
     */
    private volatile boolean activeMirror;
    private long startedAtNanos;
    private long deadlineNanos;
    private ReportAggregator aggregator;
    private BlockCheckingReport lastReport;

    public synchronized boolean start(long nowNanos, int seconds) {
        if (active) {
            return false;
        }
        active = true;
        activeMirror = true;
        startedAtNanos = nowNanos;
        deadlineNanos = nowNanos + seconds * 1_000_000_000L;
        aggregator = new ReportAggregator();
        return true;
    }

    public synchronized void recordTick(long elapsedNanos) {
        if (active) {
            aggregator.recordTick(elapsedNanos);
        }
    }

    public synchronized void recordSample(WorkObservation observation) {
        if (active) {
            aggregator.record(observation);
        }
    }

    public synchronized boolean finishIfDue(long nowNanos, int outputLimit) {
        return active && nowNanos >= deadlineNanos && finish(nowNanos, outputLimit);
    }

    public synchronized boolean stop(long nowNanos, int outputLimit) {
        return active && finish(nowNanos, outputLimit);
    }

    public boolean active() {
        return activeMirror;
    }

    public synchronized BlockCheckingReport lastReport() {
        return lastReport;
    }

    public synchronized void clear() {
        active = false;
        activeMirror = false;
        aggregator = null;
        lastReport = null;
        startedAtNanos = 0L;
        deadlineNanos = 0L;
    }

    private boolean finish(long nowNanos, int outputLimit) {
        lastReport = aggregator.finish(startedAtNanos, nowNanos, outputLimit);
        active = false;
        activeMirror = false;
        aggregator = null;
        return true;
    }
}
