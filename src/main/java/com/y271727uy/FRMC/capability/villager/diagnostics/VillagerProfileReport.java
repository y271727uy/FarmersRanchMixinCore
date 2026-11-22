package com.y271727uy.FRMC.capability.villager.diagnostics;

import java.util.List;

/** Immutable result of one villager profiling session. */
public record VillagerProfileReport(
        long wallNanos,
        int ticks,
        int villagersSeen,
        long untrackedVillagerEvents,
        long brainTicks,
        long brainNanos,
        List<Timing> phases,
        List<Timing> sensors,
        PathSummary paths,
        List<PathCaller> pathCallers,
        List<Timing> behaviorStarts,
        List<Timing> behaviorTicks,
        List<Timing> walkTargetSetters,
        List<Timing> optimizations,
        List<VillagerRow> topVillagers) {

    /** Average villager AI cost per server tick, in milliseconds. */
    public double brainMillisPerTick() {
        return ticks == 0 ? 0.0 : brainNanos / 1_000_000.0 / ticks;
    }

    public double pathMillisPerTick() {
        return ticks == 0 ? 0.0 : paths.totalNanos() / 1_000_000.0 / ticks;
    }

    public record Timing(String name, long count, long totalNanos, long maxNanos) {
        public double averageMicros() {
            return count == 0 ? 0.0 : totalNanos / 1_000.0 / count;
        }
    }

    public record PathSummary(long searches, long totalNanos, long maxNanos, long nodes, long multiTarget,
                              long none, long reached, long partial, long ignoredOffThread) {
        public double averageMicros() {
            return searches == 0 ? 0.0 : totalNanos / 1_000.0 / searches;
        }

        public double averageNodes() {
            return searches == 0 ? 0.0 : (double) nodes / searches;
        }
    }

    public record PathCaller(Timing timing, long unreached) {
    }

    /**
     * @param walkTargetSetters "SetterA x12, SetterB x3" for this villager, empty when none were seen
     * @param lastUnreached     readable sample of the latest search that did not reach, or null
     */
    public record VillagerRow(String label, long brainNanos, long pathSearches, long pathNanos, long unreached,
                              String walkTargetSetters, String lastUnreached) {
    }
}
