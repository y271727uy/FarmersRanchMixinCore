package com.y271727uy.FRMC.mixin.farmersrespite.client;

import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import umpaz.farmersrespite.client.gui.KettleRecipeBookComponent;

@Mixin(value = KettleRecipeBookComponent.class, remap = false)
public abstract class KettleRecipeBookComponentMixin {
    @ModifyConstant(method = {"setupGhostRecipe", "m_7173_"}, constant = @Constant(intValue = 2), require = 3)
    private int frmc$ghostContainerSlot(int original) {
        return KettleSlots.MENU_CONTAINER;
    }
}
