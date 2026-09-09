package com.y271727uy.FRMC.integration.geckolib.renderer.plan;

public interface BoneRenderPlanProvider {
    default BoneRenderPlan gbf$getBoneRenderPlan() {
        return BoneRenderPlan.FULL;
    }
}

