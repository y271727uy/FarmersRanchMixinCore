package com.y271727uy.FRMC.mixin.geckolib.renderer;

import com.y271727uy.FRMC.integration.acceleratedrendering.ArIntegration;
import com.y271727uy.FRMC.integration.geckolib.renderer.GeckoArModelBatch;
import com.y271727uy.FRMC.integration.geckolib.renderer.GeckoCubePose;
import com.y271727uy.FRMC.integration.geckolib.renderer.OculusCubeRenderer;
import com.y271727uy.FRMC.integration.geckolib.renderer.OculusEntityVertex;
import com.y271727uy.FRMC.integration.geckolib.renderer.EmbeddiumCubeRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.mixin.minecraft.vertex.BufferBuilderAccessorMixin;
import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

@Pseudo
@Mixin(value = GeoRenderer.class, remap = false)
public interface GeoRendererMixin {
    @Unique
    default void gbf$createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Vector3f transformed = new Vector3f();
        for (GeoVertex vertex : quad.vertices()) {
            vertex.position().mulPosition(poseState, transformed);
            buffer.vertex(transformed.x(), transformed.y(), transformed.z(), red, green, blue, alpha, vertex.texU(), vertex.texV(), packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    // Capture before Accelerated Rendering's hook at GeoBone.getCubes().
    @FabricInject(method = "renderCubesOfBone", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    default void frmc$captureBonePose(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int light,
                                      int overlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (ArIntegration.IS_INSTALLED && !bone.isHidden()
                && GeckoArModelBatch.capture((GeoRenderer<?>) (Object) this, poseStack, bone, buffer)) {
            ci.cancel();
        }
    }

    /**
     * @author 1
     * @reason 我不想写这个玩意
     */
    @Overwrite(remap = false)
    default void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float red, float green, float blue, float alpha) {
        GeckoCubePose.rotate(poseStack, cube);

        VertexBufferWriter writer = VertexBufferWriter.tryOf(buffer);
        if (writer != null) {
            if (buffer instanceof BufferBuilder bb) {
                if (((BufferBuilderAccessorMixin) bb).frmc$getFormat() == DefaultVertexFormat.NEW_ENTITY) {
                    EmbeddiumCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                    return;
                } else if (((BufferBuilderAccessorMixin) bb).frmc$getFormat() == OculusEntityVertex.FORMAT) {
                    OculusCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                    return;
                }
            } else if (buffer instanceof SodiumBufferBuilder sbb) {
                if (((BufferBuilderAccessorMixin) sbb.getOriginalBufferBuilder()).frmc$getFormat() == DefaultVertexFormat.NEW_ENTITY) {
                    EmbeddiumCubeRenderer.renderCube(poseStack, cube, writer, packedLight, packedOverlay, red, green, blue, alpha);
                    return;
                }
            }
        }

        Matrix3f normalisedPoseState = poseStack.last().normal();
        Matrix4f poseState = poseStack.last().pose();
        Vector3f normal = new Vector3f();
        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            quad.normal().mul(normalisedPoseState, normal);

            RenderUtils.fixInvertedFlatCube(cube, normal);
            gbf$createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}


