package com.y271727uy.FRMC.mixin.minecraft.recipe.accessor;

import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose private fields of Ingredient.Value subclasses.
 */
@Pseudo
@Mixin(Ingredient.ItemValue.class)
public interface ItemValueAccessor {

    @Accessor("item")
    net.minecraft.world.item.ItemStack frmc$getItem();
}
