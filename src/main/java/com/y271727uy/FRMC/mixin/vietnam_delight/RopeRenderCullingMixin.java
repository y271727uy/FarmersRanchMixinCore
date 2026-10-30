package com.y271727uy.FRMC.mixin.vietnam_delight;

import net.huwng.vietnamdelight.rope.RopeConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoid tessellating rope segments that are completely outside the render distance. */
@Pseudo
@Mixin(targets = "net.huwng.vietnamdelight.client.RopeRenderEvents", remap = false)
public abstract class RopeRenderCullingMixin {
    private static final double FRMC_ROPE_RENDER_DISTANCE_SQR = 64.0D * 64.0D;

    @Inject(
            method = "renderConnection(Lnet/huwng/vietnamdelight/rope/RopeConnection;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;DDDLnet/minecraft/client/renderer/block/BlockRenderDispatcher;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void frmc$cullDistantRope(
            RopeConnection connection,
            PoseStack poseStack,
            MultiBufferSource buffers,
            double cameraX,
            double cameraY,
            double cameraZ,
            BlockRenderDispatcher blockRenderer,
            BlockState lanternState,
            boolean ghost,
            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 firstPoint = Vec3.atCenterOf(connection.nodeA);
        Vec3 secondPoint = Vec3.atCenterOf(connection.nodeB);
        if (firstPoint.distanceToSqr(camera) > FRMC_ROPE_RENDER_DISTANCE_SQR
                && secondPoint.distanceToSqr(camera) > FRMC_ROPE_RENDER_DISTANCE_SQR) {
            ci.cancel();
        }
    }
}
