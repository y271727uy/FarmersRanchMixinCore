package com.y271727uy.FRMC.capability.villager;

import com.y271727uy.FRMC.capability.villager.diagnostics.BehaviorNames;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileLines;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileReport;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession.PathOutcome;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession.Phase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VillagerProfileSessionTest {
    @Test
    void finishesAfterRequestedTicks() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 3);
        assertFalse(session.tick());
        assertFalse(session.tick());
        assertTrue(session.tick());
    }

    @Test
    void aggregatesPathsByCallerAndVillager() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 20);
        session.recordPath(1, "farmer @ 0,64,0", "AcquirePoi", 5, 0, PathOutcome.NONE, 3_000_000L);
        session.recordPath(1, null, "AcquirePoi", 5, 12, PathOutcome.PARTIAL, 1_000_000L);
        session.recordPath(2, "none @ 5,64,5", "MoveToTargetSink", 1, 8, PathOutcome.REACHED, 500_000L);
        session.recordBrainTick(2, null, 200_000L);
        for (int i = 0; i < 20; i++) {
            session.tick();
        }

        VillagerProfileReport report = session.finish(1_000_000_000L, 8);
        VillagerProfileReport.PathSummary paths = report.paths();
        assertEquals(3, paths.searches());
        assertEquals(1, paths.none());
        assertEquals(1, paths.partial());
        assertEquals(1, paths.reached());
        assertEquals(2, paths.multiTarget());
        assertEquals(3_000_000L, paths.maxNanos());
        assertEquals(2, report.villagersSeen());

        VillagerProfileReport.PathCaller top = report.pathCallers().get(0);
        assertEquals("AcquirePoi", top.timing().name());
        assertEquals(2, top.timing().count());
        assertEquals(2, top.unreached());

        VillagerProfileReport.VillagerRow worst = report.topVillagers().get(0);
        assertEquals("farmer @ 0,64,0", worst.label(), "label from first sighting is kept");
        assertEquals(2, worst.unreached());
    }

    @Test
    void capsNamedBucketsIntoOther() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 1);
        for (int i = 0; i < VillagerProfileSession.MAX_NAMED_BUCKETS + 10; i++) {
            session.recordSensor("Sensor" + i, 1L);
        }
        session.tick();
        VillagerProfileReport report = session.finish(1L, Integer.MAX_VALUE);
        assertEquals(VillagerProfileSession.MAX_NAMED_BUCKETS + 1, report.sensors().size());
        assertTrue(report.sensors().stream().anyMatch(row -> row.name().equals(VillagerProfileSession.OTHER_BUCKET)
                && row.count() == 10));
    }

    @Test
    void capsTrackedVillagersButStillCountsTotals() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 1);
        for (int id = 0; id < VillagerProfileSession.MAX_TRACKED_VILLAGERS + 5; id++) {
            session.recordBrainTick(id, "v" + id, 10L);
        }
        session.tick();
        VillagerProfileReport report = session.finish(1L, 8);
        assertEquals(VillagerProfileSession.MAX_TRACKED_VILLAGERS, report.villagersSeen());
        assertEquals(5, report.untrackedVillagerEvents());
        assertEquals(VillagerProfileSession.MAX_TRACKED_VILLAGERS + 5L, report.brainTicks());
    }

    @Test
    void attributesWalkTargetsAndKeepsLatestUnreachedSample() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 1);
        session.recordWalkTargetSet(3, "none @ 1,1,1", "AcquirePoi");
        session.recordWalkTargetSet(3, null, "AcquirePoi");
        session.recordWalkTargetSet(3, null, "VillageBoundRandomStroll");
        session.recordUnreachedSample(3, null, "first");
        session.recordUnreachedSample(3, null, "target 9,9,9");
        session.recordBehaviorStart("MoveToTargetSink", 40L);
        session.recordBehaviorStart("OneShot:AcquirePoi", 90L);
        session.tick();

        VillagerProfileReport report = session.finish(1L, 8);
        VillagerProfileReport.VillagerRow row = report.topVillagers().get(0);
        assertEquals("AcquirePoix2, VillageBoundRandomStrollx1", row.walkTargetSetters());
        assertEquals("target 9,9,9", row.lastUnreached());
        assertEquals("AcquirePoi", report.walkTargetSetters().get(0).name());
        assertEquals(2, report.walkTargetSetters().get(0).count());
        assertEquals("OneShot:AcquirePoi", report.behaviorStarts().get(0).name(), "sorted by total time");
    }

    @Test
    void lambdaOwnersRecoverDeclarativeFactories() {
        assertEquals("AcquirePoi", BehaviorNames.lambdaOwners(
                "OneShot[M[job_site] * .map[net.minecraft.world.entity.ai.behavior.AcquirePoi$$Lambda$4512/0x0000000801a3c@1f]]"));
        assertEquals("Outer$Inner", BehaviorNames.lambdaOwners("T[a.b.Outer$Inner$$Lambda$9/0x1@2]"));
        assertEquals("SetWalkTargetFromLookTarget+StrollToPoi", BehaviorNames.lambdaOwners(
                "x.SetWalkTargetFromLookTarget$$Lambda$1@1 y.BehaviorBuilder$$Lambda$2@2 z.StrollToPoi$$Lambda$3@3"));
        assertEquals("M[walk_target]", BehaviorNames.lambdaOwners("M[walk_target]"));
    }

    @Test
    void reportLinesFormatWithRootLocale() {
        VillagerProfileSession session = new VillagerProfileSession(0L, 1);
        session.recordPhase(Phase.SENSORS, 1_500L);
        session.recordSensor("NearestBedSensor", 2_500L);
        session.recordPath(7, "librarian @ 1,2,3", "AcquirePoi", 1, 4, PathOutcome.REACHED, 10_000L);
        session.tick();
        List<VillagerProfileLines.Line> lines = VillagerProfileLines.of(session.finish(50_000_000L, 8));

        assertTrue(lines.get(0).format().startsWith("Villager profile: 1 ticks (50 ms)"));
        assertTrue(lines.stream().anyMatch(line -> line.format().contains("NearestBedSensor: n=1")));
        assertTrue(lines.stream().anyMatch(line -> line.format().contains("AcquirePoi: n=1 total=0.01 ms")));
        assertTrue(lines.stream().allMatch(line -> line.key().startsWith("frmc.villager.profile.report.")));
    }
}
