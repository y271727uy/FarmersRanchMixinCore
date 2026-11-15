package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.perf.BlockEntityRenderBox;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 方块实体视锥剔除取包围盒时走缓存；剔除判断本身仍是 Embeddium 原来的 isBoxVisible。 */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererBoxCacheMixin {
    @WrapOperation(
        method = {"renderBlockEntities", "renderGlobalBlockEntities"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getRenderBoundingBox()Lnet/minecraft/world/phys/AABB;"
        ),
        require = 0
    )
    private AABB frmc$cachedRenderBox(BlockEntity blockEntity, Operation<AABB> original) {
        return BlockEntityRenderBox.get(blockEntity, () -> original.call(blockEntity));
    }
}
