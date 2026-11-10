package com.y271727uy.FRMC.integration.kaleidoscopecookery;

import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.BambooTrayRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.BambooTrayRecipe.Subtype;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.SteamerRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModRecipes;
import com.y271727uy.FRMC.mixin.kaleidoscopecookery.IngredientInvalidationAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.RecipeManagerAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

public final class KitchenRecipeIndex {
    private static final Map<RecipeManager, SteamerIndex> STEAMERS = new WeakHashMap<>();
    private static final Map<RecipeManager, TrayIndex> TRAYS = new WeakHashMap<>();

    private KitchenRecipeIndex() {
    }

    public static boolean maySteam(RecipeManager manager, ItemStack stack) {
        Map<ResourceLocation, SteamerRecipe> recipes =
                ((RecipeManagerAccessor) manager).frmc$byType(ModRecipes.STEAMER_RECIPE);
        int tags = IngredientInvalidationAccessor.frmc$getInvalidationCounter().get();
        synchronized (STEAMERS) {
            SteamerIndex index = STEAMERS.get(manager);
            if (index == null || index.recipes != recipes || index.tags != tags) {
                index = new SteamerIndex(recipes, tags, steamableItems(recipes));
                STEAMERS.put(manager, index);
            }
            return index.inputs == null || index.inputs.contains(stack.getItem());
        }
    }

    private static Set<Item> steamableItems(Map<ResourceLocation, SteamerRecipe> recipes) {
        Set<Item> inputs = new HashSet<>();
        for (SteamerRecipe recipe : recipes.values()) {
            if (recipe.getClass() != SteamerRecipe.class || !recipe.getIngredient().isVanilla()) {
                // Custom matching may depend on stack data or the world.
                return null;
            }
            Ingredient ingredient = recipe.getIngredient();
            ingredient.checkInvalidation();
            for (ItemStack input : ingredient.getItems()) {
                inputs.add(input.getItem());
            }
        }
        return inputs;
    }

    public static List<BambooTrayRecipe> trayRecipes(RecipeManager manager, Subtype subtype) {
        Map<ResourceLocation, BambooTrayRecipe> recipes =
                ((RecipeManagerAccessor) manager).frmc$byType(ModRecipes.BAMBOO_TRAY_RECIPE);
        synchronized (TRAYS) {
            TrayIndex index = TRAYS.get(manager);
            if (index == null || index.recipes != recipes) {
                index = indexTrayRecipes(recipes);
                TRAYS.put(manager, index);
            }
            return subtype == Subtype.DRYING ? index.drying : index.wetting;
        }
    }

    private static TrayIndex indexTrayRecipes(Map<ResourceLocation, BambooTrayRecipe> recipes) {
        List<BambooTrayRecipe> drying = new ArrayList<>();
        List<BambooTrayRecipe> wetting = new ArrayList<>();
        for (BambooTrayRecipe recipe : recipes.values()) {
            if (recipe.getClass() != BambooTrayRecipe.class) {
                List<BambooTrayRecipe> all = List.copyOf(recipes.values());
                return new TrayIndex(recipes, all, all);
            }
            (recipe.getSubtype() == Subtype.DRYING ? drying : wetting).add(recipe);
        }
        return new TrayIndex(recipes, List.copyOf(drying), List.copyOf(wetting));
    }

    private record SteamerIndex(Map<ResourceLocation, SteamerRecipe> recipes, int tags, Set<Item> inputs) {
    }

    private record TrayIndex(Map<ResourceLocation, BambooTrayRecipe> recipes, List<BambooTrayRecipe> drying,
                             List<BambooTrayRecipe> wetting) {
    }
}
