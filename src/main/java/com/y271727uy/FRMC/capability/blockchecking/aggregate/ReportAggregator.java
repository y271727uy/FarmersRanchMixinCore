package com.y271727uy.FRMC.capability.blockchecking.aggregate;

import com.y271727uy.FRMC.capability.blockchecking.model.BlockCheckingReport;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkKind;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkMetric;
import com.y271727uy.FRMC.capability.blockchecking.model.WorkObservation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

/** Bounded in-memory aggregation for one block-checking session. */
public final class ReportAggregator {
    private final List<WorkObservation> samples = new ArrayList<>();
    private int discardedSamples;
    private int tickCount;

    public void record(WorkObservation observation) {
        if (samples.size() >= BlockCheckingLimits.MAX_SAMPLES) {
            discardedSamples++;
            return;
        }
        samples.add(observation);
    }

    public void recordTick(long elapsedNanos) {
        tickCount++;
        record(new WorkObservation(0L, WorkKind.SERVER_TICK, elapsedNanos, 0, 0L));
    }

    public int retainedSamples() {
        return samples.size();
    }

    public int discardedSamples() {
        return discardedSamples;
    }

    public BlockCheckingReport finish(long startedAtNanos, long finishedAtNanos, int outputLimit) {
        EnumMap<WorkKind, long[]> totals = new EnumMap<>(WorkKind.class);
        for (WorkObservation sample : samples) {
            long[] values = totals.computeIfAbsent(sample.kind(), ignored -> new long[3]);
            values[0]++;
            values[1] += sample.elapsedNanos();
            values[2] = Math.max(values[2], sample.elapsedNanos());
        }
        int boundedLimit = BlockCheckingLimits.bounded(outputLimit, 0, BlockCheckingLimits.MAX_OUTPUT_ENTRIES);
        List<WorkMetric> metrics = totals.entrySet().stream()
                .map(entry -> new WorkMetric(entry.getKey(), (int) entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .sorted(Comparator.comparingLong(WorkMetric::totalNanos).reversed().thenComparing(metric -> metric.kind().name()))
                .limit(boundedLimit)
                .toList();
        return new BlockCheckingReport(startedAtNanos, finishedAtNanos, tickCount, samples.size(), discardedSamples, metrics);
    }
}
