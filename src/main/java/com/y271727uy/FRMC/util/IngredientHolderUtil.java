package com.y271727uy.FRMC.util;

import net.minecraft.world.item.crafting.Ingredient;

public interface IngredientHolderUtil {

    static Ingredient getIngredient(Object o) {
        return ((IngredientHolderUtil) o).fastrecipesearch$getIngredient();
    }

    Ingredient fastrecipesearch$getIngredient();
}
