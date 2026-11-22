package com.y271727uy.FRMC.mixin.farmersrespite.client.jei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import umpaz.farmersrespite.integration.jei.JEIFRPlugin;

@Mixin(value = JEIFRPlugin.class, remap = false)
public abstract class JEIFRPluginMixin {
    @WrapOperation(method = "registerRecipeTransferHandlers", at = @At(value = "INVOKE", target =
            "Lmezz/jei/api/registration/IRecipeTransferRegistration;addRecipeTransferHandler("
                    + "Ljava/lang/Class;Lnet/minecraft/world/inventory/MenuType;Lmezz/jei/api/recipe/RecipeType;IIII)V"),
            require = 1)
    private void frmc$transferAllBrewIngredients(IRecipeTransferRegistration registration, Class<?> menuClass,
                                                MenuType<?> menuType, RecipeType<?> recipeType, int recipeStart,
                                                int recipeCount, int playerStart, int playerCount,
                                                Operation<Void> original) {
        original.call(registration, menuClass, menuType, recipeType, recipeStart, KettleSlots.INPUT_COUNT,
                KettleSlots.PLAYER_START, playerCount);
    }
}
