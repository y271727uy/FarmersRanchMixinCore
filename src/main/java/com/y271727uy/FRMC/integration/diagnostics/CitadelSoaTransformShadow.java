package com.y271727uy.FRMC.integration.diagnostics;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;

import java.util.IdentityHashMap;
import java.util.Map;

/** Shadow-only SoA local-to-world evaluator for Citadel measurement. */
public final class CitadelSoaTransformShadow {
    private static final Map<CitadelSkeletonLayoutCache.Layout, Workspace> WORKSPACES = new IdentityHashMap<>();
    private static final ThreadLocal<Stats> LAST_STATS = ThreadLocal.withInitial(Stats::new);

    private CitadelSoaTransformShadow() {
    }

    public static boolean lastCacheHit() {
        return LAST_STATS.get().cacheHit;
    }

    public static int lastReusedNodes() {
        return LAST_STATS.get().reusedNodes;
    }

    public static int lastReusedWorldNodes() {
        return LAST_STATS.get().reusedWorldNodes;
    }

    public static long evaluate(CitadelSkeletonLayoutCache.Layout layout) {
        Workspace workspace = WORKSPACES.computeIfAbsent(layout, ignored -> new Workspace());
        BasicModelPart[] nodes = layout.nodes();
        int count = nodes.length;
        workspace.ensure(count);
        long started = System.nanoTime();
        int[] parents = layout.parentIndices();
        int[] nodesByLevel = layout.nodesByLevel();
        float[] world = workspace.world;
        float[] local = workspace.local;
        boolean[] worldDirty = workspace.worldDirty;
        int[] poseBits = workspace.poseBits;
        int[] matrixOffsets = layout.topology().matrixOffsets();
        int[] poseOffsets = layout.topology().poseOffsets();
        int reusedNodes = 0;
        int reusedWorldNodes = 0;

        for (int order = 0; order < count; order++) {
            int i = nodesByLevel[order];
            BasicModelPart part = nodes[i];
            int poseBase = poseOffsets[i];
            boolean localChanged = !workspace.hasCachedPose || !poseMatches(part, poseBits, poseBase);
            if (localChanged) {
                capturePose(part, poseBits, poseBase);
                computeLocal(part, local, i * 12);
            } else {
                reusedNodes++;
            }

            int base = matrixOffsets[i];
            int parent = parents[i];
            boolean parentChanged = parent < 0 ? !workspace.hasCachedPose
                    : !workspace.hasCachedPose || worldDirty[parent];
            boolean nodeWorldChanged = localChanged || parentChanged;
            worldDirty[i] = nodeWorldChanged;
            if (!nodeWorldChanged) {
                reusedWorldNodes++;
                continue;
            }
            if (parent < 0) {
                System.arraycopy(local, base, world, base, 12);
                continue;
            }
            multiply(world, matrixOffsets[parent], local, base, world, base);
        }
        workspace.hasCachedPose = true;
        Stats stats = LAST_STATS.get();
        stats.reusedNodes = reusedNodes;
        stats.reusedWorldNodes = reusedWorldNodes;
        stats.cacheHit = reusedNodes == count;
        return System.nanoTime() - started;
    }

    private static void computeLocal(BasicModelPart part, float[] local, int base) {
        float px = part.rotationPointX * 0.0625F;
        float py = part.rotationPointY * 0.0625F;
        float pz = part.rotationPointZ * 0.0625F;
        float sx = scaleX(part);
        float sy = scaleY(part);
        float sz = scaleZ(part);
        float hx = part.rotateAngleX * 0.5F;
        float hy = part.rotateAngleY * 0.5F;
        float hz = part.rotateAngleZ * 0.5F;
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
        local[base] = (1.0F - 2.0F * (yy + zz)) * sx;
        local[base + 1] = (2.0F * (xy - wz)) * sy;
        local[base + 2] = (2.0F * (xz + wy)) * sz;
        local[base + 3] = px;
        local[base + 4] = (2.0F * (xy + wz)) * sx;
        local[base + 5] = (1.0F - 2.0F * (xx + zz)) * sy;
        local[base + 6] = (2.0F * (yz - wx)) * sz;
        local[base + 7] = py;
        local[base + 8] = (2.0F * (xz - wy)) * sx;
        local[base + 9] = (2.0F * (yz + wx)) * sy;
        local[base + 10] = (1.0F - 2.0F * (xx + yy)) * sz;
        local[base + 11] = pz;
    }

    private static void multiply(float[] left, int leftBase, float[] right, int rightBase,
                                 float[] output, int outputBase) {
        float a00 = left[leftBase];
        float a01 = left[leftBase + 1];
        float a02 = left[leftBase + 2];
        float a03 = left[leftBase + 3];
        float a10 = left[leftBase + 4];
        float a11 = left[leftBase + 5];
        float a12 = left[leftBase + 6];
        float a13 = left[leftBase + 7];
        float a20 = left[leftBase + 8];
        float a21 = left[leftBase + 9];
        float a22 = left[leftBase + 10];
        float a23 = left[leftBase + 11];
        float b00 = right[rightBase];
        float b01 = right[rightBase + 1];
        float b02 = right[rightBase + 2];
        float b03 = right[rightBase + 3];
        float b10 = right[rightBase + 4];
        float b11 = right[rightBase + 5];
        float b12 = right[rightBase + 6];
        float b13 = right[rightBase + 7];
        float b20 = right[rightBase + 8];
        float b21 = right[rightBase + 9];
        float b22 = right[rightBase + 10];
        float b23 = right[rightBase + 11];
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

    private static boolean poseMatches(BasicModelPart part, int[] poseBits, int base) {
        return poseBits[base] == Float.floatToIntBits(part.rotationPointX)
                && poseBits[base + 1] == Float.floatToIntBits(part.rotationPointY)
                && poseBits[base + 2] == Float.floatToIntBits(part.rotationPointZ)
                && poseBits[base + 3] == Float.floatToIntBits(part.rotateAngleX)
                && poseBits[base + 4] == Float.floatToIntBits(part.rotateAngleY)
                && poseBits[base + 5] == Float.floatToIntBits(part.rotateAngleZ)
                && poseBits[base + 6] == Float.floatToIntBits(scaleX(part))
                && poseBits[base + 7] == Float.floatToIntBits(scaleY(part))
                && poseBits[base + 8] == Float.floatToIntBits(scaleZ(part))
                && poseBits[base + 9] == (part.showModel ? 1 : 0);
    }

    private static void capturePose(BasicModelPart part, int[] poseBits, int base) {
        poseBits[base] = Float.floatToIntBits(part.rotationPointX);
        poseBits[base + 1] = Float.floatToIntBits(part.rotationPointY);
        poseBits[base + 2] = Float.floatToIntBits(part.rotationPointZ);
        poseBits[base + 3] = Float.floatToIntBits(part.rotateAngleX);
        poseBits[base + 4] = Float.floatToIntBits(part.rotateAngleY);
        poseBits[base + 5] = Float.floatToIntBits(part.rotateAngleZ);
        poseBits[base + 6] = Float.floatToIntBits(scaleX(part));
        poseBits[base + 7] = Float.floatToIntBits(scaleY(part));
        poseBits[base + 8] = Float.floatToIntBits(scaleZ(part));
        poseBits[base + 9] = part.showModel ? 1 : 0;
    }

    private static float scaleX(BasicModelPart part) {
        return part instanceof AdvancedModelBox advanced ? advanced.scaleX : 1.0F;
    }

    private static float scaleY(BasicModelPart part) {
        return part instanceof AdvancedModelBox advanced ? advanced.scaleY : 1.0F;
    }

    private static float scaleZ(BasicModelPart part) {
        return part instanceof AdvancedModelBox advanced ? advanced.scaleZ : 1.0F;
    }

    private static final class Stats {
        private boolean cacheHit;
        private int reusedNodes;
        private int reusedWorldNodes;
    }

    private static final class Workspace {
        private float[] world = new float[0];
        private float[] local = new float[0];
        private boolean[] worldDirty = new boolean[0];
        private int[] poseBits = new int[0];
        private boolean hasCachedPose;

        private void ensure(int count) {
            int required = count * 12;
            if (world.length < required) {
                world = new float[required];
            }
            if (local.length < required) {
                local = new float[required];
            }
            if (worldDirty.length < count) {
                worldDirty = new boolean[count];
                hasCachedPose = false;
            }
            int requiredPoseBits = count * 10;
            if (poseBits.length < requiredPoseBits) {
                poseBits = new int[requiredPoseBits];
                hasCachedPose = false;
            }
        }
    }
}
