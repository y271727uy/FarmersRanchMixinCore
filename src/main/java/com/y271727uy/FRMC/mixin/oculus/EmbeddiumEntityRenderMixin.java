package com.y271727uy.FRMC.mixin.oculus;

import com.bawnorton.mixinsquared.TargetHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.immediate.model.EntityRenderer;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPart.class, priority = 1500)
public abstract class EmbeddiumEntityRenderMixin {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    @Dynamic("Oculus ModelPart render handler")
    @TargetHandler(
        mixin = "net.irisshaders.iris.compat.sodium.mixin.copyEntity.ModelPartMixin",
        name = "onRender"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$letModelPartRenderWithoutShaders(
            PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha, CallbackInfo render, CallbackInfo oculus) {
        if (frmc$canUseEmbeddium() && VertexBufferWriter.tryOf(consumer) != null) {
            // Cancel only the Oculus handler so ModelPart still runs its normal traversal.
            oculus.cancel();
        }
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void frmc$compileWithEmbeddium(
            PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!frmc$canUseEmbeddium()) {
            return;
        }
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
        if (writer == null) {
            return;
        }

        EntityRenderer.prepareNormals(pose);
        int color = ColorABGR.pack(red, green, blue, alpha);
        for (int index = 0; index < cubes.size(); index++) {
            ModelPart.Cube cube = cubes.get(index);
            // Oculus already captures the same cuboid data at construction time.
            ModelCuboid cuboid = cube.getClass() == ModelPart.Cube.class && cube instanceof ModelCuboidAccessor accessor
                ? accessor.sodium$copy() : null;
            if (cuboid != null) {
                EntityRenderer.renderCuboidFast(pose, writer, cuboid, light, overlay, color);
            } else {
                cube.compile(pose, consumer, light, overlay, red, green, blue, alpha);
            }
        }
        ci.cancel();
    }

    @Unique
    private static boolean frmc$canUseEmbeddium() {
        // A null pipeline during initialization or shader reload keeps Oculus in control.
        return Iris.getPipelineManager().getPipelineNullable() instanceof VanillaRenderingPipeline;
    }
}
