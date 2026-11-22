package com.y271727uy.FRMC.mixin.farmersrespite.client.jei;

import com.y271727uy.FRMC.integration.farmersrespite.KettleRecipeFluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import umpaz.farmersrespite.common.crafting.KettlePouringRecipe;
import umpaz.farmersrespite.common.crafting.KettleRecipe;
import umpaz.farmersrespite.integration.jei.KettleUnionRecipe;

@Mixin(value = KettleUnionRecipe.class, remap = false)
public abstract class KettleUnionRecipeMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void frmc$copyFluidInputs(KettleRecipe original, KettlePouringRecipe pouring, CallbackInfo ci) {
        ((KettleRecipeFluids) (Object) this).frmc$setAdditionalInputs(
                ((KettleRecipeFluids) original).frmc$getAdditionalInputs());
    }
}
