package com.y271727uy.FRMC.mixin.minecraft.recipe.accessor;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose private fields of Ingredient.TagValue.
 */
@Pseudo
@Mixin(Ingredient.TagValue.class)
public interface TagValueAccessor {

    @Accessor("tag")
    TagKey<?> frmc$getTag();
}
