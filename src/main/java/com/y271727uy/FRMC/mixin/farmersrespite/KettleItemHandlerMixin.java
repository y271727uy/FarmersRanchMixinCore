package com.y271727uy.FRMC.mixin.farmersrespite;

import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.block.entity.inventory.KettleItemHandler;

@Mixin(value = KettleItemHandler.class, remap = false)
public abstract class KettleItemHandlerMixin {
    @Shadow @Final private IItemHandler itemHandler;
    @Shadow @Final private Direction side;

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$insertAdditionalInput(int slot, ItemStack stack, boolean simulate,
                                           CallbackInfoReturnable<ItemStack> cir) {
        if ((side == null || side == Direction.UP) && KettleSlots.isAdditionalInput(slot)) {
            cir.setReturnValue(itemHandler.insertItem(slot, stack, simulate));
        }
    }

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$extractAdditionalInput(int slot, int amount, boolean simulate,
                                            CallbackInfoReturnable<ItemStack> cir) {
        if ((side == null || side == Direction.UP) && KettleSlots.isAdditionalInput(slot)) {
            cir.setReturnValue(itemHandler.extractItem(slot, amount, simulate));
        }
    }
}
