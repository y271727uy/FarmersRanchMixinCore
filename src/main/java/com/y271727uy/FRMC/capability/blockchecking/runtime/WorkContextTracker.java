package com.y271727uy.FRMC.capability.blockchecking.runtime;

import com.y271727uy.FRMC.capability.blockchecking.aggregate.BlockCheckingLimits;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkObservation;

import java.util.Objects;

/** Fixed-slot context stack intended for low-allocation future instrumentation hooks. */
public final class WorkContextTracker {
    private final WorkKind[] kinds = new WorkKind[BlockCheckingLimits.MAX_CONTEXT_DEPTH];
    private final long[] startedAtNanos = new long[BlockCheckingLimits.MAX_CONTEXT_DEPTH];
    private final long[] revisions = new long[BlockCheckingLimits.MAX_CONTEXT_DEPTH];
    private int depth;
    private long nextRevision;

    /** Returns a revision token, or zero when the fixed stack is full. */
    public synchronized long enter(WorkKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (depth == kinds.length) {
            return 0L;
        }
        int slot = depth++;
        long revision = ++nextRevision;
        kinds[slot] = kind;
        startedAtNanos[slot] = 0L;
        revisions[slot] = revision;
        return (revision << 8) | (slot + 1L);
    }

    /** Retained for deterministic tests and callers that already have a timestamp. */
    public synchronized long enter(WorkKind kind, long nowNanos) {
        long token = enter(kind);
        if (token != 0L) {
            startedAtNanos[depth - 1] = nowNanos;
        }
        return token;
    }

    /** Removes only the exact active frame identified by {@link #enter(WorkKind, long)}. */
    public synchronized boolean exit(long token) {
        int slot = (int) (token & 0xffL) - 1;
        long revision = token >>> 8;
        if (slot < 0 || slot != depth - 1 || revisions[slot] != revision) {
            return false;
        }
        kinds[slot] = null;
        startedAtNanos[slot] = 0L;
        depth--;
        return true;
    }

    /** Creates an immutable reading only when the sampler asks for one. */
    public synchronized WorkObservation capture(long nowNanos) {
        if (depth == 0) {
            return new WorkObservation(nowNanos, WorkKind.UNKNOWN, 0L, 0, nextRevision);
        }
        int slot = depth - 1;
        if (startedAtNanos[slot] == 0L) {
            startedAtNanos[slot] = nowNanos;
        }
        return new WorkObservation(nowNanos, kinds[slot], nowNanos - startedAtNanos[slot], depth, revisions[slot]);
    }

    public synchronized int depth() {
        return depth;
    }

    public synchronized void clear() {
        for (int index = 0; index < depth; index++) {
            kinds[index] = null;
            startedAtNanos[index] = 0L;
        }
        depth = 0;
    }
}
