package com.y271727uy.FRMC.capability.blockchecking.model;

import java.util.Objects;

/** Aggregate timing statistics for one work category. */
public record WorkMetric(WorkKind kind, int samples, long totalNanos, long maximumNanos) {
    public WorkMetric {
        kind = Objects.requireNonNull(kind, "kind");
        samples = Math.max(0, samples);
        totalNanos = Math.max(0L, totalNanos);
        maximumNanos = Math.max(0L, maximumNanos);
    }

    public long averageNanos() {
        return samples == 0 ? 0L : totalNanos / samples;
    }
}
