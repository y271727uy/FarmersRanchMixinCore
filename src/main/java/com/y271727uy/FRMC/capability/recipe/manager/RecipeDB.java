package com.y271727uy.FRMC.capability.recipe.manager;

import com.google.common.base.Stopwatch;
import com.y271727uy.FRMC.capability.recipe.ingredient.RecipeSearchIntegration;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.IngredientAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.ItemValueAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.accessor.TagValueAccessor;
import com.y271727uy.computatio.api.multisetindex.FeatureMultiset;
import com.y271727uy.computatio.api.multisetindex.MultisetIndex;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class RecipeDB<C extends Container, T extends Recipe<C>> {

    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC.RecipeDB");
    private static final Comparator<RecipeHolder<?, ?>> COMPARATOR = Comparator.comparing(r -> r.id);

    private final MultisetIndex<RecipeHolder<C, T>> index;
    private final Reference2ReferenceMap<Item, int[]> hash;
    private int maxInputAmount;

    private RecipeDB(MultisetIndex<RecipeHolder<C, T>> index,
                     Reference2ReferenceMap<Item, int[]> hash,
                     int maxInputAmount) {
        this.index = index;
        this.hash = hash;
        this.maxInputAmount = maxInputAmount;
    }

    static <C extends Container, T extends Recipe<C>> RecipeDB<C, T> create(
            RecipeType<?> type, Map<ResourceLocation, T> recipes) {
        Stopwatch watch = Stopwatch.createStarted();
        List<RecipeHolder<C, T>> holders = recipes.entrySet().stream()
                .map(e -> new RecipeHolder<>(e.getKey(), e.getValue()))
                .toList();
        Reference2ReferenceMap<Item, IntSet> rawHash = new Reference2ReferenceOpenHashMap<>();
        BuildState<C, T> state = new BuildState<>(rawHash);
        MultisetIndex.Options<RecipeHolder<C, T>> options = MultisetIndex.Options.<RecipeHolder<C, T>>defaults()
                .supportsParallel(holder -> false)
                .onFeatureCountTable((holder, counts) -> holder.container = counts)
                .fallbackComparator(COMPARATOR);
        MultisetIndex<RecipeHolder<C, T>> index = MultisetIndex.build(
                holders, holder -> state.extract(holder), options);
        Reference2ReferenceMap<Item, int[]> hash = new Reference2ReferenceOpenHashMap<>();
        rawHash.forEach((item, keys) -> hash.put(item, keys.toIntArray()));
        watch.stop();
        LOGGER.info("Constructed recipe list for {} in {}. {}/{} recipes in the tree.",
                BuiltInRegistries.RECIPE_TYPE.getKey(type), watch,
                index.indexedSize(), recipes.size());
        return new RecipeDB<>(index, hash, state.maxInputAmount);
    }

    RecipeHolder<C, T> get(C inv, Level world) {
        FeatureMultiset available = extractAvailable(inv);
        return index.findAny(available, getPredicate(available, inv, world));
    }

    List<T> getAll(C inv, Level world) {
        FeatureMultiset available = extractAvailable(inv);
        return index.findAll(available, getPredicate(available, inv, world)).stream()
                .sorted(COMPARATOR)
                .map(holder -> holder.recipe)
                .collect(Collectors.toList());
    }

    private Predicate<RecipeHolder<C, T>> getPredicate(
            FeatureMultiset available, C inv, Level world) {
        if (maxInputAmount > 1) {
            return holder -> (holder.container == null || holder.container.matches(available))
                    && holder.recipe.matches(inv, world);
        }
        return holder -> holder.recipe.matches(inv, world);
    }

    private FeatureMultiset extractAvailable(C inv) {
        FeatureMultiset available = new FeatureMultiset();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            Item item = inv.getItem(i).getItem();
            if (item == Items.AIR) continue;
            int[] keys = hash.get(item);
            if (keys == null) continue;
            for (int key : keys) available.add(key, 1);
        }
        return available;
    }

    private static final class BuildState<C extends Container, T extends Recipe<C>> {
        private final Reference2ReferenceMap<Item, IntSet> rawHash;
        private int maxInputAmount;

        private BuildState(Reference2ReferenceMap<Item, IntSet> rawHash) {
            this.rawHash = rawHash;
        }

        private FeatureMultiset extract(RecipeHolder<C, T> holder) {
            if (Config.recipeSearchOptimizeOnlyVanillaRecipeClasses
                    && !holder.recipe.getClass().getName().startsWith("net.minecraft.world.item.crafting.")) {
                return new FeatureMultiset();
            }
            FeatureMultiset features = new FeatureMultiset();
            int inputAmount = 0;
            for (Ingredient ingredient : holder.recipe.getIngredients()) {
                IngredientAccessor accessor = (IngredientAccessor) ingredient;
                if (ingredient.isVanilla()) {
                    Ingredient.Value[] values = accessor.frmc$getValues();
                    if (values.length == 1) {
                        if (values[0] instanceof Ingredient.ItemValue itemValue) {
                            Item item = ((ItemValueAccessor) itemValue).frmc$getItem().getItem();
                            if (item != Items.AIR) {
                                int key = BuiltInRegistries.ITEM.getKey(item).hashCode();
                                features.add(key, 1);
                                inputAmount++;
                                rawHash.computeIfAbsent(item, ignored -> new IntOpenHashSet()).add(key);
                            }
                        } else if (values[0] instanceof Ingredient.TagValue tagValue) {
                            var tagKey = ((TagValueAccessor) tagValue).frmc$getTag();
                            @SuppressWarnings("unchecked")
                            var itemTagKey = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) tagKey;
                            var tagContents = BuiltInRegistries.ITEM.getTag(itemTagKey).orElse(null);
                            if (tagContents != null) {
                                int key = tagKey.location().hashCode();
                                features.add(key, 1);
                                inputAmount++;
                                tagContents.forEach(h -> rawHash.computeIfAbsent(h.value(), ignored -> new IntOpenHashSet()).add(key));
                            }
                        }
                    }
                } else {
                    var action = RecipeSearchIntegration.getCustomIngredientAction(ingredient.getClass());
                    if (action != null) {
                        IntSet keys = new IntOpenHashSet();
                        action.accept(ingredient, (item, key) -> {
                            keys.add(key);
                            rawHash.computeIfAbsent(item, ignored -> new IntOpenHashSet()).add(key);
                        });
                        keys.forEach(key -> features.add(key, 1));
                        if (!keys.isEmpty()) inputAmount++;
                    }
                }
            }
            maxInputAmount = Math.max(maxInputAmount, inputAmount);
            return features;
        }
    }
}
