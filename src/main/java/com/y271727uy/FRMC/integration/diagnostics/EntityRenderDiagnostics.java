package com.y271727uy.FRMC.integration.diagnostics;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.slf4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class EntityRenderDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOG_INTERVAL_NANOS = 2_000_000_000L;
    private static final AtomicLong UW_TARANTULA_CALLS = new AtomicLong();
    private static final AtomicLong UW_TARANTULA_NANOS = new AtomicLong();
    private static final AtomicLong UW_BIG_CAT_CALLS = new AtomicLong();
    private static final AtomicLong UW_BIG_CAT_NANOS = new AtomicLong();
    private static final AtomicLong AR_MESH_WRITES = new AtomicLong();
    private static final AtomicLong AR_MESH_WRITE_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_FALLBACKS = new AtomicLong();
    private static final AtomicLong CITADEL_BONE_RENDERS = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORMS = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORM_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_UNIT_SCALES = new AtomicLong();
    private static final AtomicLong CITADEL_EMPTY_GEOMETRY = new AtomicLong();
    private static final Map<AdvancedModelBox, Boolean> LOGGED_BIG_CAT_ROOTS = new IdentityHashMap<>();
    private static final AtomicLong WINDOW_START = new AtomicLong(System.nanoTime());

    private EntityRenderDiagnostics() {
    }

    public static boolean enabled() {
        return com.y271727uy.FRMC.config.Config.entityRenderDiagnosticsEnabled;
    }

    public static long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public static void logBigCatBoneTree(AdvancedModelBox root) {
        if (!enabled() || root == null || root.getParent() != null) {
            return;
        }
        Object model = root.getModel();
        if (model == null || !model.getClass().getName().contains("ModelBigCat")) {
            return;
        }
        synchronized (LOGGED_BIG_CAT_ROOTS) {
            if (LOGGED_BIG_CAT_ROOTS.put(root, Boolean.TRUE) != null) {
                return;
            }
        }

        StringBuilder tree = new StringBuilder(8192);
        int[] totals = new int[4];
        appendBoneTree(root, 0, tree, new IdentityHashMap<>(), totals);
        LOGGER.info("[FRMC/CitadelBoneTree] model={}, root={}, nodes={}, geometryNodes={}, emptyNodes={}, maxDepth={}\\n{}",
                model.getClass().getName(), root.boxName, totals[0], totals[1], totals[2], totals[3], tree);
    }

    private static void appendBoneTree(AdvancedModelBox bone, int depth, StringBuilder out,
                                       IdentityHashMap<BasicModelPart, Boolean> visited, int[] totals) {
        if (visited.put(bone, Boolean.TRUE) != null) {
            return;
        }
        totals[0]++;
        totals[3] = Math.max(totals[3], depth);
        if (bone.cubeList.isEmpty()) {
            totals[2]++;
        } else {
            totals[1]++;
        }
        out.append("  ".repeat(Math.min(depth, 64)))
                .append(bone.boxName)
                .append(" cubes=").append(bone.cubeList.size())
                .append(" children=").append(bone.childModels.size())
                .append(" visible=").append(bone.showModel)
                .append(" pos=(").append(bone.rotationPointX).append(',')
                .append(bone.rotationPointY).append(',').append(bone.rotationPointZ).append(')')
                .append(" rot=(").append(bone.rotateAngleX).append(',')
                .append(bone.rotateAngleY).append(',').append(bone.rotateAngleZ).append(')')
                .append(" scale=(").append(bone.scaleX).append(',')
                .append(bone.scaleY).append(',').append(bone.scaleZ).append(")\\n");

        ObjectList<BasicModelPart> children = bone.childModels;
        for (int i = 0, size = children.size(); i < size; i++) {
            BasicModelPart child = children.get(i);
            if (child instanceof AdvancedModelBox advancedChild) {
                appendBoneTree(advancedChild, depth + 1, out, visited, totals);
            } else {
                totals[0]++;
                totals[3] = Math.max(totals[3], depth + 1);
                out.append("  ".repeat(Math.min(depth + 1, 64)))
                        .append(child.getClass().getSimpleName())
                        .append(" cubes=? children=? (BasicModelPart)\\n");
            }
        }
    }

    public static void recordTarantula(long startedAt) {
        record(UW_TARANTULA_CALLS, UW_TARANTULA_NANOS, startedAt);
    }

    public static void recordBigCat(long startedAt) {
        record(UW_BIG_CAT_CALLS, UW_BIG_CAT_NANOS, startedAt);
    }

    public static void recordArMeshWrite(long startedAt) {
        if (!enabled() || startedAt == 0L) {
            return;
        }
        AR_MESH_WRITES.incrementAndGet();
        AR_MESH_WRITE_NANOS.addAndGet(System.nanoTime() - startedAt);
        maybeLog();
    }

    public static void recordBoneRender() {
        if (enabled()) {
            CITADEL_BONE_RENDERS.incrementAndGet();
            maybeLog();
        }
    }

    public static long startTransform() {
        if (!enabled()) {
            return 0L;
        }
        CITADEL_TRANSFORMS.incrementAndGet();
        return System.nanoTime();
    }

    public static void recordTransform(long startedAt) {
        if (enabled() && startedAt != 0L) {
            CITADEL_TRANSFORM_NANOS.addAndGet(System.nanoTime() - startedAt);
            maybeLog();
        }
    }

    public static void recordUnitScale() {
        if (enabled()) {
            CITADEL_UNIT_SCALES.incrementAndGet();
        }
    }
    public static void recordEmptyGeometry() {
        if (enabled()) {
            CITADEL_EMPTY_GEOMETRY.incrementAndGet();
        }
    }

    public static void recordCitadelFallback() {
        if (!enabled()) {
            return;
        }
        CITADEL_FALLBACKS.incrementAndGet();
        maybeLog();
    }

    private static void record(AtomicLong calls, AtomicLong nanos, long startedAt) {
        if (!enabled() || startedAt == 0L) {
            return;
        }
        calls.incrementAndGet();
        nanos.addAndGet(System.nanoTime() - startedAt);
        maybeLog();
    }

    private static void maybeLog() {
        long now = System.nanoTime();
        long start = WINDOW_START.get();
        if (now - start < LOG_INTERVAL_NANOS || !WINDOW_START.compareAndSet(start, now)) {
            return;
        }
        long tarantulaCalls = UW_TARANTULA_CALLS.getAndSet(0L);
        long tarantulaNanos = UW_TARANTULA_NANOS.getAndSet(0L);
        long bigCatCalls = UW_BIG_CAT_CALLS.getAndSet(0L);
        long bigCatNanos = UW_BIG_CAT_NANOS.getAndSet(0L);
        long arWrites = AR_MESH_WRITES.getAndSet(0L);
        long arNanos = AR_MESH_WRITE_NANOS.getAndSet(0L);
        long fallbacks = CITADEL_FALLBACKS.getAndSet(0L);
        long boneRenders = CITADEL_BONE_RENDERS.getAndSet(0L);
        long transforms = CITADEL_TRANSFORMS.getAndSet(0L);
        long transformNanos = CITADEL_TRANSFORM_NANOS.getAndSet(0L);
        long unitScales = CITADEL_UNIT_SCALES.getAndSet(0L);
        long emptyGeometry = CITADEL_EMPTY_GEOMETRY.getAndSet(0L);
        LOGGER.info("[FRMC/EntityRender] 2s tarantula.setupAnim={} calls/{} ms, bigCat.setupAnim={} calls/{} ms, ar.meshWrite={} calls/{} ms, citadelFallback={}, citadel.boneRender={}, citadel.translateRotate={} calls/{} ms, unitScale={}, emptyGeometry={}",
                tarantulaCalls, millis(tarantulaNanos), bigCatCalls, millis(bigCatNanos),
                arWrites, millis(arNanos), fallbacks, boneRenders, transforms,
                millis(transformNanos), unitScales, emptyGeometry);
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0D;
    }
}
