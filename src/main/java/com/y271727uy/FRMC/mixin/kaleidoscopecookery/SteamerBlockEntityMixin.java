package com.y271727uy.FRMC.mixin.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.SteamerBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.kaleidoscopecookery.KitchenRecipeIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SteamerBlockEntity.class)
public abstract class SteamerBlockEntityMixin {
    @Shadow(remap = false) @Final private int[] cookingTime;

    @WrapOperation(method = "cookingTick", remap = false, require = 1, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41619_()Z", ordinal = 0)})
    private boolean frmc$skipFinishedWithoutAnotherRecipe(ItemStack stack, Operation<Boolean> original,
                                                          @Local(argsOnly = true) Level level,
                                                          @Local(index = 8) int slot) {
        return original.call(stack) || cookingTime[slot] == -1 && !KitchenRecipeIndex.maySteam(level.getRecipeManager(), stack);
    }

    @WrapOperation(method = {"cookingTick", "cooldownTick"}, remap = false, require = 2, at = {
            @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/SteamerBlockEntity;setChanged(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"),
            @At(value = "INVOKE", target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/SteamerBlockEntity;m_155232_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V")})
    private void frmc$saveProgress(Level level, BlockPos pos, BlockState state, Operation<Void> original) {
        level.blockEntityChanged(pos);
    }
}
