package com.y271727uy.FRMC.mixin.embeddium.citadel;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.acceleratedrendering.citadel.CitadelEmbeddiumConfig;
import com.y271727uy.FRMC.integration.acceleratedrendering.citadel.CitadelEmbeddiumRenderer;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.FastColor.ARGB32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BasicModelPart.class, remap = false, priority = 1100)
public abstract class BasicModelPartMixin {
    @Shadow @Final private ObjectList<BasicModelPart.ModelBox> cubeList;
    @Unique private CitadelEmbeddiumRenderer.Geometry frmc$geometry;

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$renderWithEmbeddium(PoseStack.Pose pose, VertexConsumer buffer,
                                          int packedLight, int packedOverlay,
                                          float red, float green, float blue, float alpha,
                                          CallbackInfo callback) {
        if (CitadelEmbeddiumConfig.isDirectWriterEnabled()
                && CitadelEmbeddiumRenderer.isSupported(buffer)
                && !CitadelEmbeddiumRenderer.isArAccelerated(buffer)) {
            callback.cancel();
            int color = ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                    (int) (green * 255.0F), (int) (blue * 255.0F));
            if (frmc$geometry == null || !frmc$geometry.matchesCubeCount(cubeList.size())) {
                frmc$geometry = CitadelEmbeddiumRenderer.snapshot(cubeList.toArray(), false);
            }
            CitadelEmbeddiumRenderer.render(frmc$geometry, pose, buffer, packedLight, packedOverlay, color);
        }
    }
}
