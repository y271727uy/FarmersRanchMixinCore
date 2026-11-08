package com.y271727uy.FRMC.integration.diagnostics;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;

/** Immutable local pose data captured from one Citadel skeleton instance. */
public final class CitadelPoseSnapshot {
    public static final int FIELDS_PER_NODE = 10;
    private static final int VISIBLE = 1;
    private static final int SCALES_CHILDREN = 2;
    private final CitadelSkeletonLayoutCache.Topology topology;
    private final int[] poseBits;

    private CitadelPoseSnapshot(CitadelSkeletonLayoutCache.Topology topology, int[] poseBits) {
        this.topology = topology;
        this.poseBits = poseBits;
    }

    public static CitadelPoseSnapshot capture(CitadelSkeletonLayoutCache.Layout layout) {
        int[] bits = new int[layout.nodeCount() * FIELDS_PER_NODE];
        captureInto(layout, bits);
        return new CitadelPoseSnapshot(layout.topology(), bits);
    }

    public static void captureInto(CitadelSkeletonLayoutCache.Layout layout, int[] bits) {
        BasicModelPart[] nodes = layout.nodes();
        if (bits.length < nodes.length * FIELDS_PER_NODE) {
            throw new IllegalArgumentException("Pose buffer is too small");
        }
        for (int i = 0; i < nodes.length; i++) {
            BasicModelPart part = nodes[i];
            int base = i * FIELDS_PER_NODE;
            bits[base] = Float.floatToIntBits(part.rotationPointX);
            bits[base + 1] = Float.floatToIntBits(part.rotationPointY);
            bits[base + 2] = Float.floatToIntBits(part.rotationPointZ);
            bits[base + 3] = Float.floatToIntBits(part.rotateAngleX);
            bits[base + 4] = Float.floatToIntBits(part.rotateAngleY);
            bits[base + 5] = Float.floatToIntBits(part.rotateAngleZ);
            bits[base + 6] = Float.floatToIntBits(scaleX(part));
            bits[base + 7] = Float.floatToIntBits(scaleY(part));
            bits[base + 8] = Float.floatToIntBits(scaleZ(part));
            boolean scalesChildren = !(part instanceof AdvancedModelBox advanced) || advanced.scaleChildren;
            bits[base + 9] = (part.showModel ? VISIBLE : 0) | (scalesChildren ? SCALES_CHILDREN : 0);
        }
    }

    CitadelSkeletonLayoutCache.Topology topology() {
        return topology;
    }

    int bits(int node, int field) {
        return poseBits[node * FIELDS_PER_NODE + field];
    }

    float value(int node, int field) {
        return Float.intBitsToFloat(bits(node, field));
    }

    boolean visible(int node) {
        return (bits(node, 9) & VISIBLE) != 0;
    }

    boolean scalesChildren(int node) {
        return (bits(node, 9) & SCALES_CHILDREN) != 0;
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
}
