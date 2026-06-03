package com.y271727uy.FRMC.mixin.minecraft.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

/**
 * Accessor mixin to expose private methods/fields of RecipeManagement.
 */
@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {

    @Accessor("recipes")
    Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> frmc$getRecipes();

    @Invoker("byType")
    <C extends net.minecraft.world.Container, T extends Recipe<C>> Map<ResourceLocation, T> frmc$byType(RecipeType<T> recipeType);
}
