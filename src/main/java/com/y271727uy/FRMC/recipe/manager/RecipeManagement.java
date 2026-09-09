package com.y271727uy.FRMC.recipe.manager;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.polymorph.PolymorphIntegration;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.RecipeManagerAccessor;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Optimized RecipeManagement that uses a decision tree to accelerate recipe lookups.
 * <p>
 * Extends the vanilla RecipeManagement and overrides getRecipeFor/getRecipesFor
 * to use the pre-built RecipeDB decision tree instead of linear search.
 */
public class RecipeManagement extends net.minecraft.world.item.crafting.RecipeManager {
    private static final Logger LOGGER = Logger.getLogger(RecipeManagement.class.getName());

    private static final Set<RecipeType<?>> VANILLA_TYPES = Util.make(() -> {
        Set<RecipeType<?>> types = new ReferenceOpenHashSet<>();
        types.add(RecipeType.CRAFTING);
        types.add(RecipeType.SMELTING);
        types.add(RecipeType.BLASTING);
        types.add(RecipeType.SMOKING);
        types.add(RecipeType.CAMPFIRE_COOKING);
        types.add(RecipeType.STONECUTTING);
        types.add(RecipeType.SMITHING);
        return types;
    });

    private final Map<RecipeType<?>, RecipeDB<?, ?>> cachedDBMap = new ConcurrentHashMap<>();

    public RecipeManagement(net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
        super(context);
    }

    /**
     * Expose the super method for benchmarking/comparison.
     */
    public <C extends Container, T extends Recipe<C>> List<T> super_getRecipeFor(RecipeType<T> type, C input, Level world) {
        return super.getRecipesFor(type, input, world);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        super.apply(object, resourceManager, profiler);
        cachedDBMap.clear();
    }

    @Override
    public void replaceRecipes(Iterable<Recipe<?>> recipes) {
        super.replaceRecipes(recipes);
        cachedDBMap.clear();
    }

    @Override
    public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> type, C input, Level world) {
        if (Config.recipeSearchOptimizeOnlyVanilla && !VANILLA_TYPES.contains(type)) {
            return super.getRecipeFor(type, input, world);
        }
        if (PolymorphIntegration.isLoaded() && input instanceof BlockEntity blockEntity) {
            try {
                T recipe = PolymorphIntegration.getBlockEntityRecipe(type, input, world, blockEntity);
                if (recipe != null) {
                    return Optional.of(recipe);
                }
            } catch (RuntimeException exception) {
                LOGGER.log(java.util.logging.Level.WARNING, "Polymorph recipe lookup failed for " + type + ", falling back to FRMC search.", exception);
            }
        }
        var cachedRecipeList = getDB(type);
        var holder = cachedRecipeList.get(input, world);
        if (holder != null) return Optional.of(holder.recipe);
        return Optional.empty();
    }

    @Override
    public <C extends Container, T extends Recipe<C>> Optional<Pair<ResourceLocation, T>> getRecipeFor(
            RecipeType<T> type, C input, Level world, @Nullable ResourceLocation lastRecipe) {
        if (Config.recipeSearchOptimizeOnlyVanilla && !VANILLA_TYPES.contains(type)) {
            return super.getRecipeFor(type, input, world, lastRecipe);
        }
        if (PolymorphIntegration.isLoaded() && input instanceof BlockEntity blockEntity) {
            try {
                T recipe = PolymorphIntegration.getBlockEntityRecipe(type, input, world, blockEntity);
                if (recipe != null) {
                    return Optional.of(Pair.of(recipe.getId(), recipe));
                }
            } catch (RuntimeException exception) {
                LOGGER.log(java.util.logging.Level.WARNING, "Polymorph recipe lookup failed for " + type + ", falling back to FRMC search.", exception);
            }
        }
        var accessor = (RecipeManagerAccessor) this;
        Map<ResourceLocation, T> map = accessor.frmc$byType(type);
        if (lastRecipe != null) {
            T t = map.get(lastRecipe);
            if (t != null && safeMatches(t, input, world)) {
                return Optional.of(Pair.of(lastRecipe, t));
            }
        }

        var cachedRecipeList = getDB(type);
        var holder = cachedRecipeList.get(input, world);
        if (holder != null) return Optional.of(Pair.of(holder.id, holder.recipe));
        return Optional.empty();
    }

    @Override
    public <C extends Container, T extends Recipe<C>> List<T> getRecipesFor(RecipeType<T> type, C input, Level world) {
        if (Config.recipeSearchOptimizeOnlyVanilla && !VANILLA_TYPES.contains(type)) {
            return super.getRecipesFor(type, input, world);
        }
        var cachedRecipeList = getDB(type);
        return cachedRecipeList.getAll(input, world);
    }

    @SuppressWarnings("unchecked")
    private <C extends Container, T extends Recipe<C>> RecipeDB<C, T> getDB(RecipeType<T> type) {
        var accessor = (RecipeManagerAccessor) this;
        return (RecipeDB<C, T>) cachedDBMap.computeIfAbsent(type, k -> RecipeDB.create(type, accessor.frmc$byType(type)));
    }

    private static <C extends Container, T extends Recipe<C>> boolean safeMatches(T recipe, C input, Level world) {
        try {
            return recipe.matches(input, world);
        } catch (RuntimeException exception) {
            LOGGER.log(java.util.logging.Level.WARNING, "Recipe match failed in lastRecipe check, skipping.", exception);
            return false;
        }
    }
}
