package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.model.CitadelModelPart;

/** Explicit FRMC-owned Citadel traversal entry points; avoids overload ambiguity. */
public interface ICitadelFastPart extends CitadelModelPart {
    void frmc$renderFast(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                         float red, float green, float blue, float alpha);

    void frmc$renderNode(VertexConsumer buffer, PoseStack.Pose pose, int light, int overlay,
                         float red, float green, float blue, float alpha);

    void frmc$applyLocalTransform(PoseStack pose);

}
