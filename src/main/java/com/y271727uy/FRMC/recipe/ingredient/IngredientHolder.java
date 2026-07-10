package com.y271727uy.FRMC.recipe.ingredient;

import net.minecraft.world.item.crafting.Ingredient;

public interface IngredientHolder {

    static Ingredient getIngredient(Object o) {
        return ((IngredientHolder) o).fastrecipesearch$getIngredient();
    }

    Ingredient fastrecipesearch$getIngredient();
}
