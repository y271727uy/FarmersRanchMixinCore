package com.y271727uy.FRMC.recipe.manager;

import com.y271727uy.FRMC.recipe.search.IntMapContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;

final class RecipeHolder<C extends Container, T extends Recipe<C>> {
    final ResourceLocation id;
    final T recipe;
    IntMapContainer container;

    RecipeHolder(ResourceLocation id, T recipe) {
        this.id = id;
        this.recipe = recipe;
    }
}
