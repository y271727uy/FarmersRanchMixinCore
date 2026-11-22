package com.y271727uy.FRMC.mixin.farmersrespite;

import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "umpaz.farmersrespite.common.block.entity.KettleBlockEntity$1", remap = false)
public abstract class KettleInventoryChangeMixin {
    @ModifyVariable(method = "onContentsChanged", at = @At("HEAD"), argsOnly = true, require = 1)
    private int frmc$invalidateRecipeForAdditionalInputs(int slot) {
        // The original method uses the index only to decide whether to invalidate the recipe.
        return KettleSlots.isAdditionalInput(slot) ? 0 : slot;
    }
}
