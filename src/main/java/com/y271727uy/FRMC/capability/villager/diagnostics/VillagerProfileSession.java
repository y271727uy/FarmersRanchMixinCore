package com.y271727uy.FRMC.capability.villager.diagnostics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minecraft-free aggregation for one villager profiling session. Only the server thread writes to it,
 * and every map is capped so a long session in a huge village cannot grow without bound.
 */
public final class VillagerProfileSession {
    public static final int MAX_NAMED_BUCKETS = 64;
    public static final int MAX_TRACKED_VILLAGERS = 4096;
    public static final String OTHER_BUCKET = "<other>";
    public static final int MAX_SETTERS_PER_VILLAGER = 8;

    public enum Phase {
        FORGET_MEMORIES,
        SENSORS,
        START_BEHAVIORS,
        TICK_BEHAVIORS
    }

    public enum PathOutcome {
        /** Vanilla returned no path at all. */
        NONE,
        /** The path ends at an accepted target. */
        REACHED,
        /** The path stops short of every target (the search gave up or the target is enclosed). */
        PARTIAL
    }

    private final long startedAtNanos;
    private final int durationTicks;
    private int elapsedTicks;

    private long brainTicks;
    private long brainNanos;
    private final EnumMap<Phase, Metric> phases = new EnumMap<>(Phase.class);
    private final Map<String, Metric> sensors = new HashMap<>();

    private long pathSearches;
    private long pathNanos;
    private long pathMaxNanos;
    private long pathNodes;
    private long multiTargetSearches;
    private final EnumMap<PathOutcome, Long> outcomes = new EnumMap<>(PathOutcome.class);
    private final Map<String, PathMetric> pathCallers = new HashMap<>();
    /** The only counter another thread may touch. */
    private final LongAdder ignoredOffThread = new LongAdder();

    private final Map<String, Metric> behaviorStarts = new HashMap<>();
    private final Map<String, Metric> behaviorTicks = new HashMap<>();
    private final Map<String, Metric> walkTargetSetters = new HashMap<>();
    private final Map<String, Metric> optimizations = new HashMap<>();

    private final Map<Integer, VillagerMetric> villagers = new HashMap<>();
    private long untrackedVillagerEvents;

    public VillagerProfileSession(long startedAtNanos, int durationTicks) {
        this.startedAtNanos = startedAtNanos;
        this.durationTicks = Math.max(1, durationTicks);
        for (Phase phase : Phase.values()) {
            phases.put(phase, new Metric(phase.name()));
        }
        for (PathOutcome outcome : PathOutcome.values()) {
            outcomes.put(outcome, 0L);
        }
    }

    /** Counts one server tick; returns true once the requested duration is complete. */
    public boolean tick() {
        return ++elapsedTicks >= durationTicks;
    }

    /** Whether the per-villager row for this entity id already exists (lets callers skip building a label). */
    public boolean knowsVillager(int entityId) {
        return villagers.containsKey(entityId);
    }

    public void recordBrainTick(int entityId, String label, long nanos) {
        brainTicks++;
        brainNanos += nanos;
        VillagerMetric villager = villager(entityId, label);
        if (villager != null) {
            villager.brainNanos += nanos;
        }
    }

    public void recordPhase(Phase phase, long nanos) {
        phases.get(phase).add(nanos);
    }

    public void recordSensor(String sensor, long nanos) {
        bucket(sensors, sensor, Metric::new).add(nanos);
    }

    /** One tryStart call from Brain.startEachNonRunningBehavior, whether or not the behavior started. */
    public void recordBehaviorStart(String behavior, long nanos) {
        bucket(behaviorStarts, behavior, Metric::new).add(nanos);
    }

    /** One tickOrStop call from Brain.tickEachRunningBehavior. */
    public void recordBehaviorTick(String behavior, long nanos) {
        bucket(behaviorTicks, behavior, Metric::new).add(nanos);
    }

    /** Someone stored a WALK_TARGET for this villager during its brain tick. */
    public void recordWalkTargetSet(int entityId, String label, String setter) {
        bucket(walkTargetSetters, setter, Metric::new).add(0L);
        VillagerMetric villager = villager(entityId, label);
        if (villager != null) {
            String key = setter == null || setter.isEmpty() ? OTHER_BUCKET : setter;
            if (villager.setters.containsKey(key) || villager.setters.size() < MAX_SETTERS_PER_VILLAGER) {
                villager.setters.merge(key, 1L, Long::sum);
            }
        }
    }

    public void recordOptimization(String optimization) {
        bucket(optimizations, optimization, Metric::new).add(0L);
    }

    /** Remembers the most recent search that did not reach its target, as a readable sample. */
    public void recordUnreachedSample(int entityId, String label, String sample) {
        VillagerMetric villager = villager(entityId, label);
        if (villager != null) {
            villager.lastUnreached = sample;
        }
    }

    public void recordPath(int entityId, String label, String caller, int targets, int nodes,
                           PathOutcome outcome, long nanos) {
        pathSearches++;
        pathNanos += nanos;
        pathMaxNanos = Math.max(pathMaxNanos, nanos);
        pathNodes += Math.max(0, nodes);
        if (targets > 1) {
            multiTargetSearches++;
        }
        outcomes.merge(outcome, 1L, Long::sum);
        PathMetric callerMetric = bucket(pathCallers, caller, PathMetric::new);
        callerMetric.add(nanos);
        if (outcome != PathOutcome.REACHED) {
            callerMetric.unreached++;
        }
        VillagerMetric villager = villager(entityId, label);
        if (villager != null) {
            villager.pathSearches++;
            villager.pathNanos += nanos;
            if (outcome != PathOutcome.REACHED) {
                villager.unreached++;
            }
        }
    }

    public void recordOffThreadPath() {
        ignoredOffThread.increment();
    }

    public VillagerProfileReport finish(long finishedAtNanos, int topLimit) {
        List<VillagerProfileReport.Timing> phaseRows = new ArrayList<>();
        for (Metric metric : phases.values()) {
            phaseRows.add(metric.toTiming());
        }
        List<VillagerProfileReport.Timing> sensorRows = sensors.values().stream()
                .map(Metric::toTiming)
                .sorted(Comparator.comparingLong(VillagerProfileReport.Timing::totalNanos).reversed())
                .limit(topLimit)
                .toList();
        List<VillagerProfileReport.PathCaller> callerRows = pathCallers.values().stream()
                .map(metric -> new VillagerProfileReport.PathCaller(metric.toTiming(), metric.unreached))
                .sorted(Comparator.comparingLong((VillagerProfileReport.PathCaller row) -> row.timing().totalNanos()).reversed())
                .limit(topLimit)
                .toList();
        List<VillagerProfileReport.Timing> behaviorRows = behaviorStarts.values().stream()
                .map(Metric::toTiming)
                .sorted(Comparator.comparingLong(VillagerProfileReport.Timing::totalNanos).reversed())
                .limit(topLimit)
                .toList();
        List<VillagerProfileReport.Timing> behaviorTickRows = behaviorTicks.values().stream()
                .map(Metric::toTiming)
                .sorted(Comparator.comparingLong(VillagerProfileReport.Timing::totalNanos).reversed())
                .limit(topLimit)
                .toList();
        List<VillagerProfileReport.Timing> setterRows = walkTargetSetters.values().stream()
                .map(Metric::toTiming)
                .sorted(Comparator.comparingLong(VillagerProfileReport.Timing::count).reversed())
                .limit(topLimit)
                .toList();
        List<VillagerProfileReport.Timing> optimizationRows = optimizations.values().stream()
                .map(Metric::toTiming)
                .sorted(Comparator.comparingLong(VillagerProfileReport.Timing::count).reversed())
                .toList();
        List<VillagerProfileReport.VillagerRow> villagerRows = villagers.values().stream()
                .map(metric -> new VillagerProfileReport.VillagerRow(metric.label, metric.brainNanos,
                        metric.pathSearches, metric.pathNanos, metric.unreached, settersSummary(metric),
                        metric.lastUnreached))
                .sorted(Comparator.comparingLong((VillagerProfileReport.VillagerRow row) -> row.brainNanos() + row.pathNanos())
                        .reversed())
                .limit(topLimit)
                .toList();
        return new VillagerProfileReport(
                finishedAtNanos - startedAtNanos,
                elapsedTicks,
                villagers.size(),
                untrackedVillagerEvents,
                brainTicks,
                brainNanos,
                phaseRows,
                sensorRows,
                new VillagerProfileReport.PathSummary(pathSearches, pathNanos, pathMaxNanos, pathNodes,
                        multiTargetSearches, outcomes.get(PathOutcome.NONE), outcomes.get(PathOutcome.REACHED),
                        outcomes.get(PathOutcome.PARTIAL), ignoredOffThread.sum()),
                callerRows,
                behaviorRows,
                behaviorTickRows,
                setterRows,
                optimizationRows,
                villagerRows);
    }

    private static String settersSummary(VillagerMetric metric) {
        if (metric.setters.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        metric.setters.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(entry.getKey()).append('x').append(entry.getValue());
                });
        return builder.toString();
    }

    private VillagerMetric villager(int entityId, String label) {
        VillagerMetric metric = villagers.get(entityId);
        if (metric != null) {
            return metric;
        }
        if (villagers.size() >= MAX_TRACKED_VILLAGERS) {
            untrackedVillagerEvents++;
            return null;
        }
        metric = new VillagerMetric(label == null ? "#" + entityId : label);
        villagers.put(entityId, metric);
        return metric;
    }

    private static <T> T bucket(Map<String, T> map, String name, java.util.function.Function<String, T> factory) {
        String key = name == null || name.isEmpty() ? OTHER_BUCKET : name;
        T existing = map.get(key);
        if (existing != null) {
            return existing;
        }
        if (map.size() >= MAX_NAMED_BUCKETS) {
            return map.computeIfAbsent(OTHER_BUCKET, factory);
        }
        T created = factory.apply(key);
        map.put(key, created);
        return created;
    }

    private static class Metric {
        final String name;
        long count;
        long totalNanos;
        long maxNanos;

        Metric(String name) {
            this.name = name;
        }

        void add(long nanos) {
            count++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        VillagerProfileReport.Timing toTiming() {
            return new VillagerProfileReport.Timing(name, count, totalNanos, maxNanos);
        }
    }

    private static final class PathMetric extends Metric {
        long unreached;

        PathMetric(String name) {
            super(name);
        }
    }

    private static final class VillagerMetric {
        final String label;
        long brainNanos;
        long pathSearches;
        long pathNanos;
        long unreached;
        final Map<String, Long> setters = new HashMap<>();
        String lastUnreached;

        VillagerMetric(String label) {
            this.label = label;
        }
    }
}
