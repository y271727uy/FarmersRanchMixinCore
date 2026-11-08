package com.y271727uy.FRMC.integration.citadel.model;

import com.mojang.blaze3d.vertex.PoseStack;

/** Reusable identity PoseStack for building exact Citadel local transforms. */
public final class CitadelTransformScratch {
    private static final ThreadLocal<PoseStack> STACK = ThreadLocal.withInitial(PoseStack::new);

    private CitadelTransformScratch() {
    }

    public static PoseStack reset() {
        PoseStack stack = STACK.get();
        stack.last().pose().identity();
        stack.last().normal().identity();
        return stack;
    }
}
