package com.y271727uy.FRMC.recipe;

import com.google.common.base.Stopwatch;
import com.y271727uy.FRMC.mixin.minecraft.recipe.IngredientAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.ItemValueAccessor;
import com.y271727uy.FRMC.mixin.minecraft.recipe.TagValueAccessor;
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

/**
 * A recipe database that uses a decision tree to accelerate recipe lookups.
 * <p>
 * For each recipe type, it builds a tree where nodes split on item/tag hashes.
 * At query time, the input inventory's items are hashed and used to traverse
 * the tree, quickly eliminating non-matching recipes.
 */
public class RecipeDB<C extends Container, T extends Recipe<C>> extends AbstractRecipe<RecipeHolder<C, T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC.RecipeDB");

    private static final Comparator<RecipeHolder<?, ?>> COMPARATOR = Comparator.comparing(r -> r.id);

    private int maxInputAmount;
    private Reference2ReferenceMap<Item, IntSet> rawHash = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Item, int[]> hash = new Reference2ReferenceOpenHashMap<>();

    private RecipeDB() {
    }

    /**
     * Create a new RecipeDB for the given recipe type and recipes.
     */
    @SuppressWarnings("unchecked")
    public static <C extends Container, T extends Recipe<C>> RecipeDB<C, T> create(RecipeType<?> type, Map<ResourceLocation, T> recipes) {
        Stopwatch watch = Stopwatch.createStarted();
        RecipeDB<C, T> db = new RecipeDB<>();
        AbstractRecipe.build(db, recipes.entrySet().stream()
                .map(e -> new RecipeHolder<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        watch.stop();
        LOGGER.info("Constructed recipe list for {} in {}. {}/{} recipes in the tree.",
                BuiltInRegistries.RECIPE_TYPE.getKey(type), watch,
                recipes.size() - db.serialRecipes.size(), recipes.size());
        return db;
    }

    /**
     * Find a single matching recipe for the given input inventory.
     */
    public RecipeHolder<C, T> get(C inv, Level world) {
        if (this.rootBranch != null) {
            var map = extractIntMap(inv);
            if (!map.isEmpty()) {
                return findAnyMatch(map, map.toIntArray(), getPredicate(map, inv, world));
            }
        }
        return findInSerial(this.serialRecipes, getPredicate(inv, world));
    }

    /**
     * Find all matching recipes for the given input inventory.
     */
    public List<T> getAll(C inv, Level world) {
        if (this.rootBranch != null) {
            var map = extractIntMap(inv);
            if (!map.isEmpty()) {
                return search(map, map.toIntArray(), getPredicate(map, inv, world))
                        .stream()
                        .sorted(COMPARATOR)
                        .map(r -> r.recipe)
                        .collect(Collectors.toList());
            }
        }
        return serialRecipes.stream()
                .filter(getPredicate(inv, world))
                .map(r -> r.recipe)
                .collect(Collectors.toList());
    }

    private Predicate<RecipeHolder<C, T>> getPredicate(IntLongMap map, C inv, Level world) {
        if (maxInputAmount > 1) {
            return r -> {
                var c = r.container;
                return (c == null || c.match(map)) && r.recipe.matches(inv, world);
            };
        }
        return getPredicate(inv, world);
    }

    private Predicate<RecipeHolder<C, T>> getPredicate(C inv, Level world) {
        return r -> r.recipe.matches(inv, world);
    }

    /**
     * Extract an IntLongMap from the input inventory.
     * Maps each item's registry name hash to its count in the inventory.
     */
    private IntLongMap extractIntMap(C inv) {
        var map = new IntLongMap();
        var size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            var item = inv.getItem(i).getItem();
            if (item != Items.AIR) {
                var ints = hash.get(item);
                if (ints != null) {
                    for (int h : ints) {
                        map.add(h, 1);
                    }
                }
            }
        }
        return map;
    }

    @Override
    public void finishBuild() {
        super.finishBuild();
        // Convert IntSet to int[] for faster iteration
        rawHash.forEach((k, v) -> hash.put(k, v.toIntArray()));
        rawHash = null; // Allow GC
        if (!serialRecipes.isEmpty()) {
            serialRecipes.sort(COMPARATOR);
        }
    }

    @Override
    protected boolean supportsParallel(RecipeHolder<C, T> recipe) {
        return false; // All recipes go through the tree if they have ingredients
    }

    @Override
    protected IntLongMap extractIntMap(RecipeHolder<C, T> recipe) {
        var map = new IntLongMap();
        int inputAmount = 0;
        for (Ingredient ingredient : recipe.recipe.getIngredients()) {
            IngredientAccessor accessor = (IngredientAccessor) ingredient;
            if (ingredient.isVanilla()) {
                Ingredient.Value[] values = accessor.frmc$getValues();
                if (values.length == 1) {
                    if (values[0] instanceof Ingredient.ItemValue itemValue) {
                        var itemStack = ((ItemValueAccessor) itemValue).frmc$getItem();
                        var item = itemStack.getItem();
                        if (item != Items.AIR) {
                            var hash = BuiltInRegistries.ITEM.getKey(item).hashCode();
                            map.add(hash, 1);
                            inputAmount++;
                            rawHash.computeIfAbsent(item, i -> new IntOpenHashSet()).add(hash);
                        }
                    } else if (values[0] instanceof Ingredient.TagValue tagValue) {
                        var tagKey = ((TagValueAccessor) tagValue).frmc$getTag();
                        @SuppressWarnings("unchecked")
                        var itemTagKey = (net.minecraft.tags.TagKey<net.minecraft.world.item.Item>) tagKey;
                        var tagContents = BuiltInRegistries.ITEM.getTag(itemTagKey).orElse(null);
                        if (tagContents != null) {
                            var hash = tagKey.location().hashCode();
                            map.add(hash, 1);
                            inputAmount++;
                            tagContents.forEach(h -> rawHash.computeIfAbsent(h.value(), i -> new IntOpenHashSet()).add(hash));
                        }
                    }
                }
            }
            // Note: Custom ingredients (like PartialNBTIngredient) are not handled here.
            // They will fall through to serial recipe search.
        }
        maxInputAmount = Math.max(maxInputAmount, inputAmount);
        return map;
    }

    @Override
    protected void setRecipeContainer(RecipeHolder<C, T> holder, IntMapContainer container) {
        holder.container = container;
    }
}
