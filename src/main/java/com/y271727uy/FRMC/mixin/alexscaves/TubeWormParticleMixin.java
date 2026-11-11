package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.TubeWormParticle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.alexscaves.TubeWormVertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TubeWormParticle.class)
public abstract class TubeWormParticleMixin {
    @Shadow(remap = false) private BlockPos blockPos;

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), require = 1)
    private void frmc$anchorToTube(PoseStack pose, float x, float y, float z, Operation<Void> original,
            VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        // The original pose starts one block above the center of the pipe block.
        original.call(pose, (float) (blockPos.getX() + 0.5D - eye.x),
                (float) (blockPos.getY() + 1.5D - eye.y), (float) (blockPos.getZ() + 0.5D - eye.z));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/github/alexmodguy/alexscaves/client/model/TubeWormModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 1, require = 1)
    private VertexConsumer frmc$useRegularModelVertices(VertexConsumer consumer) {
        return new TubeWormVertexConsumer(consumer);
    }
}
