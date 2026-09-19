package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.BarCabinetBlockEntityRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.capability.displaywine.client.DisplayWineRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wrap {@code renderSingleBlock} 而不是 {@code defaultBlockState()}：
 * 森罗物语：兼容（kaleidoscope_compat）Redirect 了后者，两边共存会炸。
 * Wrap 这个调用点它可以共存；重复归一化是幂等的。
 */
@Pseudo
@Mixin(targets = "coffee.cypher.kaleidoscope_tavern.client.render.BarCabinetBlockEntityRender", remap = false)
public abstract class BarCabinetBlockEntityRenderMixin {
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderSingleBlock(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
                    remap = true),
            require = 0)
    private void frmc$normalizeState(
            BlockRenderDispatcher dispatcher,
            BlockState state,
            PoseStack pose,
            MultiBufferSource buffer,
            int light,
            int overlay,
            Operation<Void> original) {
        original.call(dispatcher, DisplayWineRender.normalize(state), pose, buffer, light, overlay);
    }
}
