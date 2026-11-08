package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;

/** Iterative DFS equivalent of the recursive Citadel fast renderer. */
public final class CitadelFastTraversal {
    private CitadelFastTraversal() {
    }

    public static boolean render(CitadelSkeletonLayoutCache.Layout layout, PoseStack pose, VertexConsumer buffer,
                                 int light, int overlay, float red, float green, float blue, float alpha) {
        BasicModelPart[] nodes = layout.nodes();
        int[] depths = layout.depths();
        int[] subtreeEnds = layout.topology().subtreeEnds();
        int[] flags = layout.nodeFlags();
        if (!validPlan(nodes, depths, subtreeEnds, flags)) {
            return false;
        }
        int activeDepth = -1;
        try {
            for (int i = 0; i < nodes.length; ) {
                int depth = depths[i];
                while (activeDepth >= depth) {
                    pose.popPose();
                    activeDepth--;
                }

                BasicModelPart node = nodes[i];
                if (!node.showModel) {
                    i = subtreeEnds[i];
                    continue;
                }
                int nodeFlags = flags[i];
                if ((nodeFlags & (ICitadelFastPart.FRMC_HAS_GEOMETRY | ICitadelFastPart.FRMC_HAS_CHILDREN)) == 0) {
                    i++;
                    continue;
                }
                if (!(node instanceof ICitadelFastPart fast)) {
                    node.render(pose, buffer, light, overlay, red, green, blue, alpha);
                    i = subtreeEnds[i];
                    continue;
                }

                pose.pushPose();
                activeDepth = depth;
                EntityRenderDiagnostics.recordCitadelFastNode();
                fast.frmc$applyLocalTransform(pose);
                fast.frmc$renderNode(buffer, pose.last(), light, overlay, red, green, blue, alpha);

                if ((nodeFlags & ICitadelFastPart.FRMC_ADVANCED) != 0
                        && (nodeFlags & ICitadelFastPart.FRMC_SCALE_CHILDREN) == 0) {
                    AdvancedModelBox advanced = (AdvancedModelBox) node;
                    pose.scale(1.0F / Math.max(advanced.scaleX, 1.0E-4F),
                            1.0F / Math.max(advanced.scaleY, 1.0E-4F),
                            1.0F / Math.max(advanced.scaleZ, 1.0E-4F));
                }
                i++;
            }
            return true;
        } finally {
            while (activeDepth >= 0) {
                pose.popPose();
                activeDepth--;
            }
        }
    }

    private static boolean validPlan(BasicModelPart[] nodes, int[] depths, int[] subtreeEnds, int[] flags) {
        if (nodes.length == 0 || nodes.length != depths.length
                || nodes.length != subtreeEnds.length || nodes.length != flags.length
                || depths[0] != 0) {
            return false;
        }
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null || depths[i] < 0 || subtreeEnds[i] <= i || subtreeEnds[i] > nodes.length) {
                return false;
            }
            if (i > 0 && depths[i] > depths[i - 1] + 1) {
                return false;
            }
        }
        return true;
    }
}
