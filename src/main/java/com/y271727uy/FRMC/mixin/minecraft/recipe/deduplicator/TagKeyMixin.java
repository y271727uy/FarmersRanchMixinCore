package com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator;

import com.y271727uy.FRMC.recipe.ingredient.IngredientHolder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.stream.Stream;

/**
 * Makes TagKey implement IngredientHolder so that tag-based Ingredients
 * are cached and reused instead of being created anew each time.
 */
@Pseudo
@Mixin(value = TagKey.class)
public class TagKeyMixin implements IngredientHolder {

    @Unique
    private Ingredient frmc$ingredient;

    @Override
    public Ingredient fastrecipesearch$getIngredient() {
        if (frmc$ingredient == null) {
            frmc$ingredient = Ingredient.fromValues(Stream.of(new Ingredient.TagValue((TagKey) (Object) this)));
        }
        return frmc$ingredient;
    }
}
