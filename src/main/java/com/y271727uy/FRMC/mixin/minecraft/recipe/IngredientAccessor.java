package com.y271727uy.FRMC.mixin.minecraft.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose private fields of Ingredient for recipe search optimization.
 */
@Mixin(Ingredient.class)
public interface IngredientAccessor {

    @Accessor("values")
    Ingredient.Value[] frmc$getValues();
}
