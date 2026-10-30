package com.y271727uy.FRMC.integration.ftbquests;

import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * 一般来说 mezz 应该不会动jei的api
 * */
public final class PreviewRecipeOpener {
    private PreviewRecipeOpener() {
    }

    public static boolean open(String recipeTypeId, String recipeId) {
        IJeiRuntime runtime = JEIPlugin.jeiRuntime;
        if (runtime == null || Minecraft.getInstance().player == null) {
            unavailable();
            return false;
        }
        Optional<RecipeType<?>> type = runtime.getRecipeManager().getRecipeType(new ResourceLocation(recipeTypeId));
        if (type.isEmpty()) {
            unavailable();
            return false;
        }
        return show(runtime, type.get(), recipeId);
    }

    private static <T> boolean show(IJeiRuntime runtime, RecipeType<T> type, String recipeId) {
        IRecipeManager recipes = runtime.getRecipeManager();
        IRecipeCategory<T> category = recipes.getRecipeCategory(type);
        if (!(category instanceof ModularUIRecipeCategory<?>)) {
            unavailable();
            return false;
        }
        List<T> matches = recipes.createRecipeLookup(type).get()
                .filter(recipe -> matches(category, recipe, recipeId))
                .toList();
        if (matches.isEmpty()) {
            unavailable();
            return false;
        }
        T selected = matches.get(0);
        runtime.getRecipesGui().showRecipes(category, List.of(selected), List.of());
        return true;
    }

    private static <T> boolean matches(IRecipeCategory<T> category, T recipe, String recipeId) {
        if (recipeId.isEmpty()) {
            return true;
        }
        ResourceLocation registryName = category.getRegistryName(recipe);
        return registryName != null && registryName.toString().equals(recipeId);
    }

    private static void unavailable() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("frmc.ftbquests.preview.unavailable").withStyle(ChatFormatting.RED),
                    true);
        }
    }
}
