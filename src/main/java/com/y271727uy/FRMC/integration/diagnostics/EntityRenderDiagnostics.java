package com.y271727uy.FRMC.integration.diagnostics;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class EntityRenderDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOG_INTERVAL_NANOS = 2_000_000_000L;
    private static final int HOT_PATH_SAMPLE_MASK = 63;
    private static final ThreadLocal<HotPathSample> HOT_PATH_SAMPLE = ThreadLocal.withInitial(HotPathSample::new);
    private static final AtomicLong UW_TARANTULA_CALLS = new AtomicLong();
    private static final AtomicLong UW_TARANTULA_NANOS = new AtomicLong();
    private static final AtomicLong UW_BIG_CAT_CALLS = new AtomicLong();
    private static final AtomicLong UW_BIG_CAT_NANOS = new AtomicLong();
    private static final AtomicLong AR_BEGIN_TRANSFORMS = new AtomicLong();
    private static final AtomicLong AR_BEGIN_TRANSFORM_NANOS = new AtomicLong();
    private static final AtomicLong AR_MESH_SUBMISSIONS = new AtomicLong();
    private static final AtomicLong AR_MESH_SUBMISSION_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_FALLBACKS = new AtomicLong();
    private static final AtomicLong CITADEL_FAST_ROOTS = new AtomicLong();
    private static final AtomicLong CITADEL_FAST_NODES = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORM_CACHE_HITS = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORM_CACHE_MISSES = new AtomicLong();
    private static final AtomicLong CITADEL_POSE_GROUP_HITS = new AtomicLong();
    private static final AtomicLong CITADEL_POSE_GROUP_MISSES = new AtomicLong();
    private static final ThreadLocal<FastPathSample> FAST_PATH_SAMPLE = ThreadLocal.withInitial(FastPathSample::new);
    private static final AtomicLong CITADEL_BONE_RENDERS = new AtomicLong();
    private static final AtomicLong CITADEL_PART_RENDERS = new AtomicLong();
    private static final AtomicLong CITADEL_PART_RENDER_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_ROOT_RENDERS = new AtomicLong();
    private static final AtomicLong CITADEL_ROOT_RENDER_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_ROOT_PARTS = new AtomicLong();
    private static final AtomicLong CITADEL_ROOT_MIN_PARTS = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong CITADEL_ROOT_MAX_PARTS = new AtomicLong();
    private static final ThreadLocal<RootRenderSample> ROOT_RENDER_SAMPLE = ThreadLocal.withInitial(RootRenderSample::new);
    private static final AtomicLong CITADEL_MESH_RECORDS = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORMS = new AtomicLong();
    private static final AtomicLong CITADEL_MESH_CACHE_BUILDS = new AtomicLong();
    private static final AtomicLong CITADEL_MESH_CACHE_HITS = new AtomicLong();
    private static final AtomicLong CITADEL_TRANSFORM_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_UNIT_SCALES = new AtomicLong();
    private static final AtomicLong CITADEL_EMPTY_GEOMETRY = new AtomicLong();
    private static final AtomicLong CITADEL_LAYOUT_BUILDS = new AtomicLong();
    private static final AtomicLong CITADEL_LOCAL_DIRTY = new AtomicLong();
    private static final AtomicLong CITADEL_LOCAL_CLEAN = new AtomicLong();
    private static final AtomicLong CITADEL_WORLD_DIRTY = new AtomicLong();
    private static final AtomicLong CITADEL_WORLD_CLEAN = new AtomicLong();
    private static final AtomicLong CITADEL_SOA_SHADOW_CALLS = new AtomicLong();
    private static final AtomicLong CITADEL_SOA_SHADOW_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_SOA_SHADOW_CACHE_HITS = new AtomicLong();
    private static final AtomicLong CITADEL_SOA_REUSED_NODES = new AtomicLong();
    private static final AtomicLong CITADEL_SOA_REUSED_WORLD_NODES = new AtomicLong();
    private static final int CITADEL_BATCH_SIZE = 32;
    private static final Map<CitadelSkeletonLayoutCache.Topology, List<CitadelPoseSnapshot>> POSE_BATCHES = new IdentityHashMap<>();
    private static final AtomicLong CITADEL_BATCH_COUNT = new AtomicLong();
    private static final AtomicLong CITADEL_BATCH_ENTITIES = new AtomicLong();
    private static final AtomicLong CITADEL_BATCH_NANOS = new AtomicLong();
    private static final AtomicLong CITADEL_BATCH_MAX_ERROR_SCALED = new AtomicLong();
    private static final Map<BasicModelPart, Boolean> WORLD_DIRTY_THIS_FRAME = new IdentityHashMap<>();
    private static final AtomicLong FRAME_COUNT = new AtomicLong();
    private static final Map<AdvancedModelBox, Boolean> LOGGED_BIG_CAT_ROOTS = new IdentityHashMap<>();
    private static final Map<BasicModelPart, PoseSnapshot> POSE_SNAPSHOTS = new IdentityHashMap<>();
    private static final Map<BasicModelPart, Long> POSE_SEEN_FRAME = new IdentityHashMap<>();
    private static final AtomicLong FRAME_SEQUENCE = new AtomicLong();
    private static final AtomicLong WINDOW_START = new AtomicLong(System.nanoTime());

    private EntityRenderDiagnostics() {
    }

    public static boolean enabled() {
        return com.y271727uy.FRMC.config.Config.entityRenderDiagnosticsEnabled;
    }

    public static void recordFrame() {
        if (enabled()) {
            FRAME_COUNT.incrementAndGet();
            FRAME_SEQUENCE.incrementAndGet();
            WORLD_DIRTY_THIS_FRAME.clear();
            POSE_BATCHES.clear();
        }
    }

    public static long start() {
        return enabled() ? System.nanoTime() : 0L;
    }

    public static void recordSoaShadow(AdvancedModelBox root) {
        if (!enabled() || root == null || root.getParent() != null) {
            return;
        }
        boolean soaShadow = Config.citadelSoaShadowEnabled;
        boolean gpuValidation = Config.citadelGpuPoseValidationEnabled && CitadelGpuPoseValidation.pending();
        if (!soaShadow && !gpuValidation) {
            return;
        }
        CitadelSkeletonLayoutCache.Layout layout = CitadelSkeletonLayoutCache.getOrBuild(root);
        if (soaShadow) {
            long nanos = CitadelSoaTransformShadow.evaluate(layout);
            CITADEL_SOA_SHADOW_CALLS.incrementAndGet();
            if (CitadelSoaTransformShadow.lastCacheHit()) {
                CITADEL_SOA_SHADOW_CACHE_HITS.incrementAndGet();
            }
            CITADEL_SOA_REUSED_NODES.addAndGet(CitadelSoaTransformShadow.lastReusedNodes());
            CITADEL_SOA_REUSED_WORLD_NODES.addAndGet(CitadelSoaTransformShadow.lastReusedWorldNodes());
            CITADEL_SOA_SHADOW_NANOS.addAndGet(nanos);
        }
        boolean cpuValidation = soaShadow && Config.citadelBatchValidationEnabled;
        if (cpuValidation || (gpuValidation && layout.nodeCount() <= CitadelGpuPoseValidation.MAX_NODES)) {
            recordBatchSnapshot(layout, cpuValidation, gpuValidation);
        }
    }

    private static void recordBatchSnapshot(CitadelSkeletonLayoutCache.Layout layout,
                                            boolean cpuValidation, boolean gpuValidation) {
        CitadelSkeletonLayoutCache.Topology topology = layout.topology();
        List<CitadelPoseSnapshot> batch = POSE_BATCHES.computeIfAbsent(topology, ignored -> new ArrayList<>(CITADEL_BATCH_SIZE));
        batch.add(CitadelPoseSnapshot.capture(layout));
        if (batch.size() < CITADEL_BATCH_SIZE) {
            return;
        }
        if (cpuValidation) {
            CitadelBatchTransformShadow.Result result = CitadelBatchTransformShadow.evaluateAndCompare(batch);
            CITADEL_BATCH_COUNT.incrementAndGet();
            CITADEL_BATCH_ENTITIES.addAndGet(result.entityCount());
            CITADEL_BATCH_NANOS.addAndGet(result.nanos());
            long scaledError = Math.round(result.maxError() * 1_000_000_000.0D);
            CITADEL_BATCH_MAX_ERROR_SCALED.updateAndGet(previous -> Math.max(previous, scaledError));
        }
        if (gpuValidation && layout.nodeCount() <= CitadelGpuPoseValidation.MAX_NODES) {
            CitadelGpuPoseValidation.validate(batch);
        }
        batch.clear();
    }

    public static void logBigCatBoneTree(AdvancedModelBox root) {
        if (!enabled() || !LOGGER.isDebugEnabled() || root == null || root.getParent() != null) {
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

        CitadelSkeletonLayoutCache.Layout layout = CitadelSkeletonLayoutCache.getOrBuild(root);
        CITADEL_LAYOUT_BUILDS.incrementAndGet();
        StringBuilder tree = new StringBuilder(8192);
        int[] totals = new int[4];
        appendBoneTree(root, 0, tree, new IdentityHashMap<>(), totals);
        LOGGER.debug("[FRMC/CitadelBoneTree] model={}, root={}, nodes={}, layoutNodes={}, geometryNodes={}, emptyNodes={}, maxDepth={}\\n{}",
                model.getClass().getName(), root.boxName, totals[0], layout.nodeCount(), totals[1], totals[2], totals[3], tree);
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

    public static void recordArBeginTransform(long startedAt) {
        record(AR_BEGIN_TRANSFORMS, AR_BEGIN_TRANSFORM_NANOS, startedAt);
    }

    public static void recordArMeshSubmission(long startedAt) {
        record(AR_MESH_SUBMISSIONS, AR_MESH_SUBMISSION_NANOS, startedAt);
    }

    public static void recordArMeshWrite(long startedAt) {
        recordArMeshSubmission(startedAt);
    }

    public static long startCitadelPartRender() {
        return enabled() && (HOT_PATH_SAMPLE.get().partRenders++ & HOT_PATH_SAMPLE_MASK) == 0
                ? System.nanoTime() : 0L;
    }

    public static void recordCitadelPartRender(long startedAt) {
        if (startedAt == 0L) {
            return;
        }
        CITADEL_PART_RENDERS.addAndGet(HOT_PATH_SAMPLE_MASK + 1L);
        CITADEL_PART_RENDER_NANOS.addAndGet((System.nanoTime() - startedAt) * (HOT_PATH_SAMPLE_MASK + 1L));
    }

    public static void beginCitadelRootRender(boolean accelerated) {
        if (!enabled() || !accelerated) {
            return;
        }
        RootRenderSample sample = ROOT_RENDER_SAMPLE.get();
        sample.active = true;
        sample.startedAt = System.nanoTime();
        sample.parts = 0;
    }

    public static void finishCitadelRootRender() {
        RootRenderSample sample = ROOT_RENDER_SAMPLE.get();
        if (!enabled() || !sample.active) {
            return;
        }
        long parts = sample.parts;
        CITADEL_ROOT_RENDERS.incrementAndGet();
        CITADEL_ROOT_RENDER_NANOS.addAndGet(System.nanoTime() - sample.startedAt);
        CITADEL_ROOT_PARTS.addAndGet(parts);
        CITADEL_ROOT_MIN_PARTS.accumulateAndGet(parts, Math::min);
        CITADEL_ROOT_MAX_PARTS.accumulateAndGet(parts, Math::max);
        sample.active = false;
    }

    public static void recordBoneRender() {
        if (enabled()) {
            CITADEL_BONE_RENDERS.incrementAndGet();
            RootRenderSample sample = ROOT_RENDER_SAMPLE.get();
            if (sample.active) {
                sample.parts++;
            }
            maybeLog();
        }
    }

    public static void recordMeshRecord() {
        if (enabled() && (HOT_PATH_SAMPLE.get().meshRecords++ & HOT_PATH_SAMPLE_MASK) == 0) {
            CITADEL_MESH_RECORDS.addAndGet(HOT_PATH_SAMPLE_MASK + 1L);
        }
    }

    public static void recordMeshCacheBuild() {
        if (enabled() && (HOT_PATH_SAMPLE.get().meshBuilds++ & HOT_PATH_SAMPLE_MASK) == 0) {
            CITADEL_MESH_CACHE_BUILDS.addAndGet(HOT_PATH_SAMPLE_MASK + 1L);
        }
    }

    public static void recordMeshCacheHit() {
        if (enabled() && (HOT_PATH_SAMPLE.get().meshHits++ & HOT_PATH_SAMPLE_MASK) == 0) {
            CITADEL_MESH_CACHE_HITS.addAndGet(HOT_PATH_SAMPLE_MASK + 1L);
        }
    }

    public static long startTransform() {
        if (!enabled()) {
            return 0L;
        }
        CITADEL_TRANSFORMS.incrementAndGet();
        return System.nanoTime();
    }

    public static void recordTransform(BasicModelPart part, long startedAt) {
        recordTransform(startedAt);
        recordLocalPose(part);
    }

    public static void recordTransform(long startedAt) {
        if (enabled() && startedAt != 0L) {
            CITADEL_TRANSFORM_NANOS.addAndGet(System.nanoTime() - startedAt);
            maybeLog();
        }
    }

    private static void recordLocalPose(BasicModelPart part) {
        if (!enabled() || part == null) {
            return;
        }
        long frame = FRAME_SEQUENCE.get();
        Long previousFrame = POSE_SEEN_FRAME.put(part, frame);
        if (previousFrame != null && previousFrame == frame) {
            return;
        }
        PoseSnapshot current = new PoseSnapshot(part);
        PoseSnapshot previous = POSE_SNAPSHOTS.put(part, current);
        boolean localDirty = previous == null || !previous.sameAs(current);
        if (localDirty) {
            CITADEL_LOCAL_DIRTY.incrementAndGet();
        } else {
            CITADEL_LOCAL_CLEAN.incrementAndGet();
        }
        BasicModelPart parent = CitadelSkeletonLayoutCache.parentOf(part);
        boolean worldDirty = localDirty || Boolean.TRUE.equals(WORLD_DIRTY_THIS_FRAME.get(parent));
        WORLD_DIRTY_THIS_FRAME.put(part, worldDirty);
        if (worldDirty) {
            CITADEL_WORLD_DIRTY.incrementAndGet();
        } else {
            CITADEL_WORLD_CLEAN.incrementAndGet();
        }
    }

    private static final class PoseSnapshot {
        private final float pointX;
        private final float pointY;
        private final float pointZ;
        private final float angleX;
        private final float angleY;
        private final float angleZ;
        private final float scaleX;
        private final float scaleY;
        private final float scaleZ;
        private final boolean visible;

        private PoseSnapshot(BasicModelPart part) {
            pointX = part.rotationPointX;
            pointY = part.rotationPointY;
            pointZ = part.rotationPointZ;
            angleX = part.rotateAngleX;
            angleY = part.rotateAngleY;
            angleZ = part.rotateAngleZ;
            if (part instanceof AdvancedModelBox advanced) {
                scaleX = advanced.scaleX;
                scaleY = advanced.scaleY;
                scaleZ = advanced.scaleZ;
            } else {
                scaleX = 1.0F;
                scaleY = 1.0F;
                scaleZ = 1.0F;
            }
            visible = part.showModel;
        }

        private boolean sameAs(PoseSnapshot other) {
            return Float.floatToIntBits(pointX) == Float.floatToIntBits(other.pointX)
                    && Float.floatToIntBits(pointY) == Float.floatToIntBits(other.pointY)
                    && Float.floatToIntBits(pointZ) == Float.floatToIntBits(other.pointZ)
                    && Float.floatToIntBits(angleX) == Float.floatToIntBits(other.angleX)
                    && Float.floatToIntBits(angleY) == Float.floatToIntBits(other.angleY)
                    && Float.floatToIntBits(angleZ) == Float.floatToIntBits(other.angleZ)
                    && Float.floatToIntBits(scaleX) == Float.floatToIntBits(other.scaleX)
                    && Float.floatToIntBits(scaleY) == Float.floatToIntBits(other.scaleY)
                    && Float.floatToIntBits(scaleZ) == Float.floatToIntBits(other.scaleZ)
                    && visible == other.visible;
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

    public static void recordCitadelFastRoot() {
        if (enabled()) {
            CITADEL_FAST_ROOTS.incrementAndGet();
        }
    }

    public static void recordCitadelFastNode() {
        if (enabled()) {
            FAST_PATH_SAMPLE.get().nodes++;
        }
    }

    public static void recordCitadelPoseGroup(boolean reused) {
        if (!enabled()) {
            return;
        }
        FastPathSample sample = FAST_PATH_SAMPLE.get();
        if (reused) {
            sample.poseGroupHits++;
        } else {
            sample.poseGroupMisses++;
        }
    }

    public static void recordCitadelTransformCache(boolean hit) {
        if (!enabled()) {
            return;
        }
        FastPathSample sample = FAST_PATH_SAMPLE.get();
        if (hit) {
            sample.transformHits++;
        } else {
            sample.transformMisses++;
        }
    }

    public static void finishCitadelFastRoot() {
        if (!enabled()) {
            return;
        }
        FastPathSample sample = FAST_PATH_SAMPLE.get();
        CITADEL_FAST_NODES.addAndGet(sample.nodes);
        CITADEL_TRANSFORM_CACHE_HITS.addAndGet(sample.transformHits);
        CITADEL_TRANSFORM_CACHE_MISSES.addAndGet(sample.transformMisses);
        CITADEL_POSE_GROUP_HITS.addAndGet(sample.poseGroupHits);
        CITADEL_POSE_GROUP_MISSES.addAndGet(sample.poseGroupMisses);
        sample.nodes = 0L;
        sample.transformHits = 0L;
        sample.transformMisses = 0L;
        sample.poseGroupHits = 0L;
        sample.poseGroupMisses = 0L;
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
        long windowNanos = Math.max(1L, now - start);
        long frames = FRAME_COUNT.getAndSet(0L);
        long tarantulaCalls = UW_TARANTULA_CALLS.getAndSet(0L);
        long tarantulaNanos = UW_TARANTULA_NANOS.getAndSet(0L);
        long bigCatCalls = UW_BIG_CAT_CALLS.getAndSet(0L);
        long bigCatNanos = UW_BIG_CAT_NANOS.getAndSet(0L);
        long arBeginTransforms = AR_BEGIN_TRANSFORMS.getAndSet(0L);
        long arBeginTransformNanos = AR_BEGIN_TRANSFORM_NANOS.getAndSet(0L);
        long arMeshSubmissions = AR_MESH_SUBMISSIONS.getAndSet(0L);
        long arMeshSubmissionNanos = AR_MESH_SUBMISSION_NANOS.getAndSet(0L);
        long fallbacks = CITADEL_FALLBACKS.getAndSet(0L);
        long fastRoots = CITADEL_FAST_ROOTS.getAndSet(0L);
        long fastNodes = CITADEL_FAST_NODES.getAndSet(0L);
        long transformCacheHits = CITADEL_TRANSFORM_CACHE_HITS.getAndSet(0L);
        long transformCacheMisses = CITADEL_TRANSFORM_CACHE_MISSES.getAndSet(0L);
        long poseGroupHits = CITADEL_POSE_GROUP_HITS.getAndSet(0L);
        long poseGroupMisses = CITADEL_POSE_GROUP_MISSES.getAndSet(0L);
        long boneRenders = CITADEL_BONE_RENDERS.getAndSet(0L);
        long partRenders = CITADEL_PART_RENDERS.getAndSet(0L);
        long partRenderNanos = CITADEL_PART_RENDER_NANOS.getAndSet(0L);
        long rootRenders = CITADEL_ROOT_RENDERS.getAndSet(0L);
        long rootRenderNanos = CITADEL_ROOT_RENDER_NANOS.getAndSet(0L);
        long rootParts = CITADEL_ROOT_PARTS.getAndSet(0L);
        long rootMinParts = rootRenders == 0L ? 0L : CITADEL_ROOT_MIN_PARTS.getAndSet(Long.MAX_VALUE);
        long rootMaxParts = CITADEL_ROOT_MAX_PARTS.getAndSet(0L);
        long transforms = CITADEL_TRANSFORMS.getAndSet(0L);
        long meshRecords = CITADEL_MESH_RECORDS.getAndSet(0L);
        long meshCacheBuilds = CITADEL_MESH_CACHE_BUILDS.getAndSet(0L);
        long meshCacheHits = CITADEL_MESH_CACHE_HITS.getAndSet(0L);
        long transformNanos = CITADEL_TRANSFORM_NANOS.getAndSet(0L);
        long unitScales = CITADEL_UNIT_SCALES.getAndSet(0L);
        long emptyGeometry = CITADEL_EMPTY_GEOMETRY.getAndSet(0L);
        long layoutBuilds = CITADEL_LAYOUT_BUILDS.getAndSet(0L);
        long localDirty = CITADEL_LOCAL_DIRTY.getAndSet(0L);
        long localClean = CITADEL_LOCAL_CLEAN.getAndSet(0L);
        long worldDirty = CITADEL_WORLD_DIRTY.getAndSet(0L);
        long worldClean = CITADEL_WORLD_CLEAN.getAndSet(0L);
        long soaCalls = CITADEL_SOA_SHADOW_CALLS.getAndSet(0L);
        long soaNanos = CITADEL_SOA_SHADOW_NANOS.getAndSet(0L);
        long soaCacheHits = CITADEL_SOA_SHADOW_CACHE_HITS.getAndSet(0L);
        long soaReusedNodes = CITADEL_SOA_REUSED_NODES.getAndSet(0L);
        long soaReusedWorldNodes = CITADEL_SOA_REUSED_WORLD_NODES.getAndSet(0L);
        long batchCount = CITADEL_BATCH_COUNT.getAndSet(0L);
        long batchEntities = CITADEL_BATCH_ENTITIES.getAndSet(0L);
        long batchNanos = CITADEL_BATCH_NANOS.getAndSet(0L);
        long batchMaxErrorScaled = CITADEL_BATCH_MAX_ERROR_SCALED.getAndSet(0L);
        double fps = frames * 1_000_000_000.0D / windowNanos;
        double transformAvgNs = transforms == 0L ? 0.0D : (double) transformNanos / transforms;
        double partRenderAvgNs = partRenders == 0L ? 0.0D : (double) partRenderNanos / partRenders;
        double rootRenderAvgNs = rootRenders == 0L ? 0.0D : (double) rootRenderNanos / rootRenders;
        double rootPartsAvg = rootRenders == 0L ? 0.0D : (double) rootParts / rootRenders;
        double arBeginTransformAvgNs = arBeginTransforms == 0L ? 0.0D
                : (double) arBeginTransformNanos / arBeginTransforms;
        double arMeshSubmissionAvgNs = arMeshSubmissions == 0L ? 0.0D
                : (double) arMeshSubmissionNanos / arMeshSubmissions;
        long localSamples = localDirty + localClean;
        long worldSamples = worldDirty + worldClean;
        double dirtyPercent = localSamples == 0L ? 0.0D : localDirty * 100.0D / localSamples;
        double worldDirtyPercent = worldSamples == 0L ? 0.0D : worldDirty * 100.0D / worldSamples;
        double soaAvgNs = soaCalls == 0L ? 0.0D : (double) soaNanos / soaCalls;
        double batchAvgNs = batchCount == 0L ? 0.0D : (double) batchNanos / batchCount;
        double batchPerEntityNs = batchEntities == 0L ? 0.0D : (double) batchNanos / batchEntities;
        LOGGER.info("[FRMC/EntityRender] 2s mode={}, fps={}, frames={}, windowMs={}, tarantula.setupAnim={} calls/{} ms, bigCat.setupAnim={} calls/{} ms, ar.beginTransform={} calls/{} ms/{} ns avg, ar.meshSubmit={} calls/{} ms/{} ns avg, citadelFallback={}, citadel.boneRender={}, citadel.rootRender={} roots/{} ms/{} ns avg, advancedVisits/root={} avg (min={}, max={}), citadel.partRender={} calls/{} ms/{} ns avg, citadel.transformRecords={}, citadel.meshRecords={} (meshInfo proxy), meshCacheBuilds={}, meshCacheHits={}, citadel.translateRotate={} calls/{} ms/{} ns avg, unitScale={}, emptyGeometry={}, layoutBuilds={}, localDirty={}/{} ({}%), localClean={}, worldDirty={}/{} ({}%), worldClean={}, soaShadow={} calls/{} ns avg (cacheHits={}, reusedNodes={}, reusedWorldNodes={}), batch={} batches/{} entities/{} ns avg/{} ns perEntity/maxError={}e-9",
                com.y271727uy.FRMC.config.Config.citadelCombinedRotationEnabled ? "combined" : "vanilla",
                round(fps), frames, windowNanos / 1_000_000L,
                tarantulaCalls, millis(tarantulaNanos), bigCatCalls, millis(bigCatNanos),
                arBeginTransforms, millis(arBeginTransformNanos), round(arBeginTransformAvgNs), arMeshSubmissions, millis(arMeshSubmissionNanos), round(arMeshSubmissionAvgNs), fallbacks, boneRenders, rootRenders, millis(rootRenderNanos), round(rootRenderAvgNs), round(rootPartsAvg), rootMinParts, rootMaxParts, partRenders, millis(partRenderNanos), round(partRenderAvgNs), transforms, meshRecords,
                meshCacheBuilds, meshCacheHits,
                transforms, millis(transformNanos), round(transformAvgNs), unitScales, emptyGeometry,
                layoutBuilds, localDirty, localSamples, round(dirtyPercent), localClean,
                worldDirty, worldSamples, round(worldDirtyPercent), worldClean, soaCalls, round(soaAvgNs), soaCacheHits, soaReusedNodes, soaReusedWorldNodes,
                 batchCount, batchEntities, round(batchAvgNs), round(batchPerEntityNs), batchMaxErrorScaled);
        LOGGER.info("[FRMC/CitadelFast] 2s fastRoots={}, fastNodes={}, transformCacheHits={}, transformCacheMisses={}, poseGroupHits={}, poseGroupMisses={}, fastTraversal={}",
                fastRoots, fastNodes, transformCacheHits, transformCacheMisses, poseGroupHits, poseGroupMisses,
                com.y271727uy.FRMC.config.Config.citadelFastTraversalEnabled);
    }

    private static final class HotPathSample {
        private int partRenders;
        private int meshRecords;
        private int meshBuilds;
        private int meshHits;
    }

    private static final class FastPathSample {
        private long nodes;
        private long transformHits;
        private long transformMisses;
        private long poseGroupHits;
        private long poseGroupMisses;
    }

    private static final class RootRenderSample {
        private boolean active;
        private long startedAt;
        private long parts;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0D;
    }
}
