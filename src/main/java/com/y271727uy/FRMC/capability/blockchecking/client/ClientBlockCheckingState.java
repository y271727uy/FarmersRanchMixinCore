package com.y271727uy.FRMC.capability.blockchecking.client;

import com.y271727uy.FRMC.capability.blockchecking.model.ChunkPressureSnapshot;
import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;

/** Client-side immutable view of the latest server block-checking packets. */
public final class ClientBlockCheckingState {
    private ChunkPressureSnapshot snapshot;
    private BlockCheckingReport report;
    private long snapshotRevision = -1L;
    private long snapshotTimestamp;
    private long reportRevision = -1L;
    private long reportTimestamp;

    public synchronized boolean updateSnapshot(long revision, long timestamp, ChunkPressureSnapshot value) {
        if (revision < snapshotRevision || (revision == snapshotRevision && timestamp <= snapshotTimestamp)) {
            return false;
        }
        snapshotRevision = revision;
        snapshotTimestamp = timestamp;
        snapshot = value;
        return true;
    }

    public synchronized boolean updateReport(long revision, long timestamp, BlockCheckingReport value) {
        if (revision < reportRevision || (revision == reportRevision && timestamp <= reportTimestamp)) {
            return false;
        }
        reportRevision = revision;
        reportTimestamp = timestamp;
        report = value;
        return true;
    }

    public synchronized ChunkPressureSnapshot snapshot() {
        return snapshot;
    }

    public synchronized BlockCheckingReport report() {
        return report;
    }

    public synchronized long snapshotRevision() {
        return snapshotRevision;
    }

    public synchronized long reportRevision() {
        return reportRevision;
    }

    public synchronized void invalidateSnapshot() {
        snapshot = null;
        snapshotRevision = -1L;
        snapshotTimestamp = 0L;
    }

    public synchronized void clear() {
        snapshot = null;
        report = null;
        snapshotRevision = -1L;
        snapshotTimestamp = 0L;
        reportRevision = -1L;
        reportTimestamp = 0L;
    }
}
