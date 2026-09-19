package com.y271727uy.FRMC.capability.blockchecking.runtime;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.ReportAggregator;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkObservation;

/** Thread-safe lifecycle for the sole active block-checking session. */
public final class BlockCheckingSession {
    private boolean active;
    private long startedAtNanos;
    private long deadlineNanos;
    private ReportAggregator aggregator;
    private BlockCheckingReport lastReport;

    public synchronized boolean start(long nowNanos, int seconds) {
        if (active) {
            return false;
        }
        active = true;
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

    public synchronized boolean active() {
        return active;
    }

    public synchronized BlockCheckingReport lastReport() {
        return lastReport;
    }

    public synchronized void clear() {
        active = false;
        aggregator = null;
        lastReport = null;
        startedAtNanos = 0L;
        deadlineNanos = 0L;
    }

    private boolean finish(long nowNanos, int outputLimit) {
        lastReport = aggregator.finish(startedAtNanos, nowNanos, outputLimit);
        active = false;
        aggregator = null;
        return true;
    }
}
