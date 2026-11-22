package com.y271727uy.FRMC.mixin.yeastnfeast;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.registry.BnCItems;

@Pseudo
@Mixin(targets = "net.astralya.yeastnfeast.item.custom.MeadItem", remap = false)
public abstract class MeadItemMixin {
    @Inject(method = "getReturnContainer", at = @At("RETURN"), cancellable = true)
    private void frmc$returnTankardAfterDrinking(ItemStack mead, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(new ItemStack(BnCItems.TANKARD.get()));
    }
}
