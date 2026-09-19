package com.y271727uy.FRMC.capability.blockchecking.model;

import java.util.Objects;

/** Immutable point-in-time reading produced by the block-checking sampler. */
public record WorkObservation(long capturedAtNanos, WorkKind kind, long elapsedNanos, int depth, long revision) {
    public WorkObservation {
        kind = Objects.requireNonNull(kind, "kind");
        elapsedNanos = Math.max(0L, elapsedNanos);
        depth = Math.max(0, depth);
        revision = Math.max(0L, revision);
    }
}
