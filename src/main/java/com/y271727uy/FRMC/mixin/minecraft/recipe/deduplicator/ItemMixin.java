package com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator;

import com.y271727uy.FRMC.recipe.ingredient.IngredientHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.stream.Stream;

/**
 * Makes Item implement IngredientHolder so that single-item Ingredients
 * are cached and reused instead of being created anew each time.
 */
@Pseudo
@Mixin(Item.class)
public abstract class ItemMixin implements ItemLike, IngredientHolder {

    @Unique
    private Ingredient frmc$ingredient;

    @Override
    public Ingredient fastrecipesearch$getIngredient() {
        if (frmc$ingredient == null) {
            frmc$ingredient = Ingredient.fromValues(Stream.of(new Ingredient.ItemValue(new ItemStack(this))));
        }
        return frmc$ingredient;
    }
}
