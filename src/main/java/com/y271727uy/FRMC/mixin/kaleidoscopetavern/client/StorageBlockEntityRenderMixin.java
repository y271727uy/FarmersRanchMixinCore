package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.StorageBlockEntityRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.displaywine.client.DisplayWineRender;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "coffee.cypher.kaleidoscope_tavern.client.render.StorageBlockEntityRender", remap = false)
public abstract class StorageBlockEntityRenderMixin {
    @WrapOperation(
            method = "renderStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = true),
            require = 0)
    private BlockState frmc$normalizeState(Block block, Operation<BlockState> original) {
        return DisplayWineRender.normalize(original.call(block));
    }
}
