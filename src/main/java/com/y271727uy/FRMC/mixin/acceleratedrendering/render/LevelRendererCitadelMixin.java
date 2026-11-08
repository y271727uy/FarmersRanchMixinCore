package com.y271727uy.FRMC.mixin.acceleratedrendering.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGeometryVisibility;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelModelLod;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCitadelMixin {
    @WrapMethod(method = "renderEntity", require = 0)
    private void frmc$scopeWorldEntity(Entity entity, double cameraX, double cameraY, double cameraZ,
                                       float partialTick, PoseStack poses, MultiBufferSource buffers,
                                       Operation<Void> original) {
        boolean previous = CitadelModelLod.setWorldEntity(Config.citadelGeneralLodEnabled);
        try {
            original.call(entity, cameraX, cameraY, cameraZ, partialTick, poses, buffers);
        } finally {
            CitadelModelLod.setWorldEntity(previous);
        }
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void frmc$resetTerrainDepth(CallbackInfo ci) {
        CitadelGeometryVisibility.beginFrame();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            ordinal = 2, shift = At.Shift.AFTER), require = 0)
    private void frmc$captureTerrainDepth(PoseStack poses, float partialTick, long finishNanoTime, boolean blockOutline,
                                          Camera camera, GameRenderer gameRenderer, LightTexture lightTexture,
                                          Matrix4f projection, CallbackInfo ci) {
        CitadelGeometryVisibility.captureTerrainDepth(projection);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void frmc$finishBottomFaceSample(CallbackInfo ci) {
        CitadelGeometryVisibility.finishFrame();
    }
}
