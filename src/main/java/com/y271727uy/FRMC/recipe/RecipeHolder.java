package com.y271727uy.FRMC.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Holds a recipe along with its pre-computed IntMapContainer for fast matching.
 */
public class RecipeHolder<C extends Container, T extends Recipe<C>> {
    public final ResourceLocation id;
    public final T recipe;
    public IntMapContainer container;

    public RecipeHolder(ResourceLocation id, T recipe) {
        this.id = id;
        this.recipe = recipe;
    }
}
