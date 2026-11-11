package com.y271727uy.FRMC.integration.acceleratedrendering;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public final class MaidBoneGeometry implements IGeoRenderer<Void> {
    private static final MaidBoneGeometry INSTANCE = new MaidBoneGeometry();

    private MaidBoneGeometry() {}

    public static void emit(AnimatedGeoBone bone, VertexConsumer writer) {
        // Bake the existing GeckoLib 3 geometry in bone-local space.
        INSTANCE.renderCubesOfBone(bone, new PoseStack(), writer, 0, 0, 1, 1, 1, 1);
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        throw new UnsupportedOperationException("Bone baking uses the supplied vertex consumer");
    }

    @Override
    public ResourceLocation getTextureLocation(Void unused) {
        throw new UnsupportedOperationException("Bone baking uses the active render type");
    }
}
