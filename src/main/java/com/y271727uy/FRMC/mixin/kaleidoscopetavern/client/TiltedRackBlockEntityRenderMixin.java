package com.y271727uy.FRMC.mixin.kaleidoscopetavern.client;

import com.github.ysbbbbbb.kaleidoscopetavern.client.render.block.TiltedRackBlockEntityRender;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.displaywine.client.DisplayWineRender;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TiltedRackBlockEntityRender.class, remap = false)
public abstract class TiltedRackBlockEntityRenderMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;", remap = true))
    private Item frmc$displayBottle(ItemStack stack, Operation<Item> original) {
        return DisplayWineRender.bottleItem(stack, original.call(stack));
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = true),
            require = 0)
    private BlockState frmc$normalizeState(Block block, Operation<BlockState> original) {
        return DisplayWineRender.normalize(original.call(block));
    }
}
