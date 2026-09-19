package com.y271727uy.FRMC.capability.blockchecking.model;

import java.util.List;

/** Immutable result retained after a block-checking session ends. */
public record BlockCheckingReport(long startedAtNanos, long finishedAtNanos, int tickCount,
                                int retainedSamples, int discardedSamples, List<WorkMetric> metrics) {
    public BlockCheckingReport {
        finishedAtNanos = Math.max(startedAtNanos, finishedAtNanos);
        tickCount = Math.max(0, tickCount);
        retainedSamples = Math.max(0, retainedSamples);
        discardedSamples = Math.max(0, discardedSamples);
        metrics = List.copyOf(metrics);
    }

    public long durationNanos() {
        return finishedAtNanos - startedAtNanos;
    }
}
