package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import com.y271727uy.FRMC.integration.citadel.model.CitadelTransformScratch;
import com.y271727uy.FRMC.integration.diagnostics.CitadelPoseSnapshot;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Bounded exact full-pose cache shared across identical Citadel skeleton instances. */
public final class CitadelPoseSharingCache {
    private static final int CAPACITY = 16;
    private static final ThreadLocal<Workspace> WORKSPACE = ThreadLocal.withInitial(Workspace::new);

    private CitadelPoseSharingCache() {
    }

    public static Pose acquire(CitadelSkeletonLayoutCache.Layout layout) {
        Workspace workspace = WORKSPACE.get();
        int required = layout.nodeCount() * CitadelPoseSnapshot.FIELDS_PER_NODE;
        workspace.ensure(required);
        CitadelPoseSnapshot.captureInto(layout, workspace.current);
        boolean combinedRotation = Config.citadelCombinedRotationEnabled;
        int hash = hash(workspace.current, required, combinedRotation);
        for (Slot slot : workspace.slots) {
            if (!slot.valid || slot.topology != layout.topology() || slot.hash != hash
                    || slot.length != required || slot.combinedRotation != combinedRotation
                    || !samePose(slot.poseBits, workspace.current, required)) {
                continue;
            }
            slot.reused = true;
            return slot;
        }

        Slot slot = workspace.slots[workspace.nextSlot++ & (CAPACITY - 1)];
        slot.ensureNodes(layout.nodeCount());
        slot.topology = layout.topology();
        slot.hash = hash;
        slot.length = required;
        slot.combinedRotation = combinedRotation;
        slot.reused = false;
        System.arraycopy(workspace.current, 0, slot.poseBits, 0, required);

        BasicModelPart[] nodes = layout.nodes();
        for (int i = 0; i < nodes.length; i++) {
            BasicModelPart node = nodes[i];
            PoseStack scratch = CitadelTransformScratch.reset();
            if (node instanceof ICitadelFastPart fast) {
                fast.frmc$applyLocalTransform(scratch);
            } else if (node instanceof AdvancedModelBox advanced) {
                advanced.translateAndRotate(scratch);
            } else {
                node.translateRotate(scratch);
            }
            slot.localPose[i].set(scratch.last().pose());
            slot.localNormal[i].set(scratch.last().normal());
        }
        slot.valid = true;
        return slot;
    }

    public static void clear() {
        WORKSPACE.remove();
    }

    private static boolean samePose(int[] first, int[] second, int length) {
        for (int i = 0; i < length; i++) {
            if (first[i] != second[i]) {
                return false;
            }
        }
        return true;
    }

    private static int hash(int[] values, int length, boolean combinedRotation) {
        int hash = combinedRotation ? 0x811c9dc5 : 0x4f1bbcdc;
        for (int i = 0; i < length; i++) {
            hash ^= values[i];
            hash *= 0x01000193;
        }
        return hash;
    }

    public static class Pose {
        protected Matrix4f[] localPose;
        protected Matrix3f[] localNormal;
        protected boolean reused;

        private Pose(int nodeCount) {
            this.localPose = new Matrix4f[nodeCount];
            this.localNormal = new Matrix3f[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                localPose[i] = new Matrix4f();
                localNormal[i] = new Matrix3f();
            }
        }

        public Matrix4f localPose(int node) {
            return localPose[node];
        }

        public Matrix3f localNormal(int node) {
            return localNormal[node];
        }

        public boolean reused() {
            return reused;
        }
    }

    private static final class Slot extends Pose {
        private CitadelSkeletonLayoutCache.Topology topology;
        private int hash;
        private int length;
        private boolean combinedRotation;
        private boolean valid;
        private int[] poseBits = new int[0];

        private Slot() {
            super(0);
        }

        private void ensureNodes(int nodeCount) {
            if (localPose.length == nodeCount) {
                if (poseBits.length < nodeCount * CitadelPoseSnapshot.FIELDS_PER_NODE) {
                    poseBits = new int[nodeCount * CitadelPoseSnapshot.FIELDS_PER_NODE];
                }
                return;
            }
            localPose = new Matrix4f[nodeCount];
            localNormal = new Matrix3f[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                localPose[i] = new Matrix4f();
                localNormal[i] = new Matrix3f();
            }
            poseBits = new int[nodeCount * CitadelPoseSnapshot.FIELDS_PER_NODE];
        }
    }

    private static final class Workspace {
        private final Slot[] slots = new Slot[CAPACITY];
        private int[] current = new int[0];
        private int nextSlot;

        private Workspace() {
            for (int i = 0; i < slots.length; i++) {
                slots[i] = new Slot();
            }
        }

        private void ensure(int required) {
            if (current.length < required) {
                current = new int[required];
            }
        }
    }
}
