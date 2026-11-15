package com.y271727uy.FRMC.mixin.citadel.model;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.render.embeddium.CitadelEmbeddiumConfig;
import com.y271727uy.FRMC.integration.citadel.render.embeddium.CitadelEmbeddiumRenderer;
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
public abstract class EmbeddiumBasicModelPartMixin {
    @Shadow @Final private ObjectList<BasicModelPart.ModelBox> cubeList;
    @Unique private CitadelEmbeddiumRenderer.Geometry frmc$geometry;
    @Unique private boolean frmc$geometryFailed;

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$renderWithEmbeddium(PoseStack.Pose pose, VertexConsumer buffer,
                                          int packedLight, int packedOverlay,
                                          float red, float green, float blue, float alpha,
                                          CallbackInfo callback) {
        if (CitadelEmbeddiumConfig.isDirectWriterEnabled()
                && CitadelEmbeddiumRenderer.isSupported(buffer)
                && !CitadelEmbeddiumRenderer.isArAccelerated(buffer)) {
            if (frmc$geometryFailed) {
                return;
            }
            if (frmc$geometry == null || !frmc$geometry.matchesCubes(cubeList)) {
                try {
                    frmc$geometry = CitadelEmbeddiumRenderer.snapshot(cubeList.toArray(), false);
                } catch (RuntimeException | LinkageError ignored) {
                    // 读不出几何就交还给 Citadel 自己的提交路径，不再重试。
                    frmc$geometry = null;
                    frmc$geometryFailed = true;
                    return;
                }
            }
            callback.cancel();
            int color = ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                    (int) (green * 255.0F), (int) (blue * 255.0F));
            CitadelEmbeddiumRenderer.render(frmc$geometry, pose, buffer, packedLight, packedOverlay, color);
        }
    }
}
