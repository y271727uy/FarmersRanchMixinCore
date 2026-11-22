package com.y271727uy.FRMC.capability.villager.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a report into translation keys plus pre-formatted arguments. The English template doubles as the
 * log line, so the server log and the chat output always show the same numbers.
 */
public final class VillagerProfileLines {
    private static final String PREFIX = "frmc.villager.profile.report.";

    private VillagerProfileLines() {
    }

    public record Line(String key, String english, Object... args) {
        public String format() {
            return String.format(Locale.ROOT, english, args);
        }
    }

    public static List<Line> of(VillagerProfileReport report) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(PREFIX + "header",
                "Villager profile: %s ticks (%s ms), villagers=%s, brain ticks=%s, brain=%s ms/tick",
                report.ticks(), report.wallNanos() / 1_000_000L, report.villagersSeen(), report.brainTicks(),
                fixed(report.brainMillisPerTick())));
        for (VillagerProfileReport.Timing phase : report.phases()) {
            lines.add(timing("phase", "  phase %s: n=%s total=%s ms avg=%s us max=%s us", phase));
        }
        if (!report.sensors().isEmpty()) {
            lines.add(new Line(PREFIX + "sensors", "Sensor scans that ran:"));
            for (VillagerProfileReport.Timing sensor : report.sensors()) {
                lines.add(timing("timing", "  %s: n=%s total=%s ms avg=%s us max=%s us", sensor));
            }
        }
        VillagerProfileReport.PathSummary paths = report.paths();
        lines.add(new Line(PREFIX + "paths",
                "Vanilla path searches: n=%s, %s ms/tick, avg=%s us, max=%s ms, avg nodes=%s",
                paths.searches(), fixed(report.pathMillisPerTick()), fixed(paths.averageMicros()),
                fixed(paths.maxNanos() / 1_000_000.0), fixed(paths.averageNodes())));
        lines.add(new Line(PREFIX + "outcomes",
                "  reached=%s partial=%s none=%s multi-target=%s off-thread(ignored)=%s",
                paths.reached(), paths.partial(), paths.none(), paths.multiTarget(), paths.ignoredOffThread()));
        if (!report.pathCallers().isEmpty()) {
            lines.add(new Line(PREFIX + "callers", "Path searches by caller:"));
            for (VillagerProfileReport.PathCaller caller : report.pathCallers()) {
                VillagerProfileReport.Timing timing = caller.timing();
                lines.add(new Line(PREFIX + "caller", "  %s: n=%s total=%s ms avg=%s us unreached=%s",
                        timing.name(), timing.count(), millis(timing.totalNanos()), fixed(timing.averageMicros()),
                        caller.unreached()));
            }
        }
        if (!report.behaviorStarts().isEmpty()) {
            lines.add(new Line(PREFIX + "behaviors", "Behavior start checks (START_BEHAVIORS split):"));
            for (VillagerProfileReport.Timing behavior : report.behaviorStarts()) {
                lines.add(timing("timing", "  %s: n=%s total=%s ms avg=%s us max=%s us", behavior));
            }
        }
        if (!report.behaviorTicks().isEmpty()) {
            lines.add(new Line(PREFIX + "behavior_ticks", "Running behavior ticks (TICK_BEHAVIORS split):"));
            for (VillagerProfileReport.Timing behavior : report.behaviorTicks()) {
                lines.add(timing("timing", "  %s: n=%s total=%s ms avg=%s us max=%s us", behavior));
            }
        }
        if (!report.walkTargetSetters().isEmpty()) {
            lines.add(new Line(PREFIX + "setters", "Walk target set by:"));
            for (VillagerProfileReport.Timing setter : report.walkTargetSetters()) {
                lines.add(new Line(PREFIX + "setter", "  %s: n=%s", setter.name(), setter.count()));
            }
        }
        if (!report.optimizations().isEmpty()) {
            lines.add(new Line(PREFIX + "optimizations", "FRMC optimizations applied:"));
            for (VillagerProfileReport.Timing optimization : report.optimizations()) {
                lines.add(new Line(PREFIX + "setter", "  %s: n=%s", optimization.name(), optimization.count()));
            }
        }
        if (!report.topVillagers().isEmpty()) {
            lines.add(new Line(PREFIX + "villagers", "Most expensive villagers:"));
            for (VillagerProfileReport.VillagerRow row : report.topVillagers()) {
                lines.add(new Line(PREFIX + "villager", "  %s: brain=%s ms, paths=%s (%s ms, unreached=%s)",
                        row.label(), millis(row.brainNanos()), row.pathSearches(), millis(row.pathNanos()),
                        row.unreached()));
                if (!row.walkTargetSetters().isEmpty()) {
                    lines.add(new Line(PREFIX + "villager.setters", "    walk target from: %s", row.walkTargetSetters()));
                }
                if (row.lastUnreached() != null) {
                    lines.add(new Line(PREFIX + "villager.unreached", "    last unreached: %s", row.lastUnreached()));
                }
            }
        }
        if (report.untrackedVillagerEvents() > 0) {
            lines.add(new Line(PREFIX + "untracked", "  %s events from villagers beyond the per-villager table cap",
                    report.untrackedVillagerEvents()));
        }
        lines.add(new Line(PREFIX + "scope",
                "Path time requested by behaviors is already part of brain time. Read-only: villager behavior is unchanged."));
        return lines;
    }

    private static Line timing(String kind, String english, VillagerProfileReport.Timing timing) {
        return new Line(PREFIX + kind, english, timing.name(), timing.count(), millis(timing.totalNanos()),
                fixed(timing.averageMicros()), fixed(timing.maxNanos() / 1_000.0));
    }

    private static String millis(long nanos) {
        return fixed(nanos / 1_000_000.0);
    }

    private static String fixed(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
