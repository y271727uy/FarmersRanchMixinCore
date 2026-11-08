package com.y271727uy.FRMC.integration.diagnostics;

import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import java.util.List;

/** Level-major CPU batch evaluator for immutable Citadel pose snapshots. */
public final class CitadelBatchTransformShadow {
    private static final ThreadLocal<Workspace> WORKSPACE = ThreadLocal.withInitial(Workspace::new);

    private CitadelBatchTransformShadow() {
    }

    public static Result evaluate(List<CitadelPoseSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return new Result(0L, 0, 0, new float[0], 0.0F);
        }
        CitadelPoseSnapshot[] poses = snapshots.toArray(new CitadelPoseSnapshot[0]);
        CitadelSkeletonLayoutCache.Topology topology = poses[0].topology();
        int entityCount = poses.length;
        int nodeCount = topology.nodeCount();
        for (int entity = 0; entity < entityCount; entity++) {
            requireTopology(poses[entity], topology);
        }
        Workspace workspace = WORKSPACE.get();
        workspace.ensure(entityCount * nodeCount * 12);
        float[] world = workspace.world;
        int[] parents = topology.parentIndices();
        int[] nodesByLevel = topology.nodesByLevel();
        int[] levelStarts = topology.levelStarts();
        long started = System.nanoTime();
        for (int level = 0; level < levelStarts.length - 1; level++) {
            for (int order = levelStarts[level]; order < levelStarts[level + 1]; order++) {
                int node = nodesByLevel[order];
                int nodeBase = node * entityCount * 12;
                int parent = parents[node];
                int parentBase = parent < 0 ? -1 : parent * entityCount * 12;
                for (int entity = 0; entity < entityCount; entity++) {
                    CitadelPoseSnapshot pose = poses[entity];
                    int base = nodeBase + entity * 12;
                    writeLocal(pose, node, world, base);
                    if (parent >= 0) {
                        multiply(world, parentBase + entity * 12, world, base, world, base);
                    }
                }
            }
        }
        return new Result(System.nanoTime() - started, entityCount, nodeCount, world, 0.0F);
    }

    public static Result evaluateAndCompare(List<CitadelPoseSnapshot> snapshots) {
        Result batch = evaluate(snapshots);
        if (batch.entityCount == 0) {
            return batch;
        }
        CitadelPoseSnapshot[] poses = snapshots.toArray(new CitadelPoseSnapshot[0]);
        CitadelSkeletonLayoutCache.Topology topology = poses[0].topology();
        Workspace workspace = WORKSPACE.get();
        workspace.ensure(batch.world.length);
        float[] sequential = workspace.reference;
        int[] parents = topology.parentIndices();
        int[] nodesByLevel = topology.nodesByLevel();
        int[] levelStarts = topology.levelStarts();
        for (int entity = 0; entity < batch.entityCount; entity++) {
            CitadelPoseSnapshot snapshot = poses[entity];
            for (int level = 0; level < levelStarts.length - 1; level++) {
                for (int order = levelStarts[level]; order < levelStarts[level + 1]; order++) {
                    int node = nodesByLevel[order];
                    int base = node * batch.entityCount * 12 + entity * 12;
                    writeLocal(snapshot, node, sequential, base);
                    int parent = parents[node];
                    if (parent >= 0) {
                        multiply(sequential, parent * batch.entityCount * 12 + entity * 12,
                                sequential, base, sequential, base);
                    }
                }
            }
        }
        float maxError = 0.0F;
        for (int i = 0; i < batch.entityCount * batch.nodeCount * 12; i++) {
            maxError = Math.max(maxError, Math.abs(sequential[i] - batch.world[i]));
        }
        return new Result(batch.nanos, batch.entityCount, batch.nodeCount, batch.world, maxError);
    }

    private static void requireTopology(CitadelPoseSnapshot snapshot,
                                        CitadelSkeletonLayoutCache.Topology topology) {
        if (snapshot.topology() != topology) {
            throw new IllegalArgumentException("Mismatched Citadel skeleton topology in batch");
        }
    }

    private static final class Workspace {
        private float[] world = new float[0];
        private float[] reference = new float[0];

        private void ensure(int required) {
            if (world.length < required) {
                world = new float[required];
            }
            if (reference.length < required) {
                reference = new float[required];
            }
        }
    }

    private static void writeLocal(CitadelPoseSnapshot pose, int node, float[] target, int base) {
        float px = pose.value(node, 0) * 0.0625F;
        float py = pose.value(node, 1) * 0.0625F;
        float pz = pose.value(node, 2) * 0.0625F;
        float sx = pose.value(node, 6);
        float sy = pose.value(node, 7);
        float sz = pose.value(node, 8);
        float hx = pose.value(node, 3) * 0.5F;
        float hy = pose.value(node, 4) * 0.5F;
        float hz = pose.value(node, 5) * 0.5F;
        float cx = (float) Math.cos(hx);
        float sxr = (float) Math.sin(hx);
        float cy = (float) Math.cos(hy);
        float syr = (float) Math.sin(hy);
        float cz = (float) Math.cos(hz);
        float szr = (float) Math.sin(hz);
        float qw = cz * cy * cx + szr * syr * sxr;
        float qx = cz * cy * sxr - szr * syr * cx;
        float qy = cz * syr * cx + szr * cy * sxr;
        float qz = szr * cy * cx - cz * syr * sxr;
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float wx = qw * qx;
        float wy = qw * qy;
        float wz = qw * qz;
        target[base] = (1.0F - 2.0F * (yy + zz)) * sx;
        target[base + 1] = (2.0F * (xy - wz)) * sy;
        target[base + 2] = (2.0F * (xz + wy)) * sz;
        target[base + 3] = px;
        target[base + 4] = (2.0F * (xy + wz)) * sx;
        target[base + 5] = (1.0F - 2.0F * (xx + zz)) * sy;
        target[base + 6] = (2.0F * (yz - wx)) * sz;
        target[base + 7] = py;
        target[base + 8] = (2.0F * (xz - wy)) * sx;
        target[base + 9] = (2.0F * (yz + wx)) * sy;
        target[base + 10] = (1.0F - 2.0F * (xx + yy)) * sz;
        target[base + 11] = pz;
    }

    private static void multiply(float[] left, int leftBase, float[] right, int rightBase,
                                 float[] output, int outputBase) {
        float a00 = left[leftBase], a01 = left[leftBase + 1], a02 = left[leftBase + 2], a03 = left[leftBase + 3];
        float a10 = left[leftBase + 4], a11 = left[leftBase + 5], a12 = left[leftBase + 6], a13 = left[leftBase + 7];
        float a20 = left[leftBase + 8], a21 = left[leftBase + 9], a22 = left[leftBase + 10], a23 = left[leftBase + 11];
        float b00 = right[rightBase], b01 = right[rightBase + 1], b02 = right[rightBase + 2], b03 = right[rightBase + 3];
        float b10 = right[rightBase + 4], b11 = right[rightBase + 5], b12 = right[rightBase + 6], b13 = right[rightBase + 7];
        float b20 = right[rightBase + 8], b21 = right[rightBase + 9], b22 = right[rightBase + 10], b23 = right[rightBase + 11];
        output[outputBase] = a00 * b00 + a01 * b10 + a02 * b20;
        output[outputBase + 1] = a00 * b01 + a01 * b11 + a02 * b21;
        output[outputBase + 2] = a00 * b02 + a01 * b12 + a02 * b22;
        output[outputBase + 3] = a00 * b03 + a01 * b13 + a02 * b23 + a03;
        output[outputBase + 4] = a10 * b00 + a11 * b10 + a12 * b20;
        output[outputBase + 5] = a10 * b01 + a11 * b11 + a12 * b21;
        output[outputBase + 6] = a10 * b02 + a11 * b12 + a12 * b22;
        output[outputBase + 7] = a10 * b03 + a11 * b13 + a12 * b23 + a13;
        output[outputBase + 8] = a20 * b00 + a21 * b10 + a22 * b20;
        output[outputBase + 9] = a20 * b01 + a21 * b11 + a22 * b21;
        output[outputBase + 10] = a20 * b02 + a21 * b12 + a22 * b22;
        output[outputBase + 11] = a20 * b03 + a21 * b13 + a22 * b23 + a23;
    }

    public static final class Result {
        private final long nanos;
        private final int entityCount;
        private final int nodeCount;
        private final float[] world;
        private final float maxError;

        private Result(long nanos, int entityCount, int nodeCount, float[] world, float maxError) {
            this.nanos = nanos;
            this.entityCount = entityCount;
            this.nodeCount = nodeCount;
            this.world = world;
            this.maxError = maxError;
        }

        public long nanos() { return nanos; }
        public int entityCount() { return entityCount; }
        public int nodeCount() { return nodeCount; }
        public float maxError() { return maxError; }
    }
}
