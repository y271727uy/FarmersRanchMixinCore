package com.y271727uy.FRMC.recipe.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.computatio.api.multisetindex.FeatureCounts;
import com.y271727uy.computatio.api.multisetindex.FeatureMultiset;
import com.y271727uy.computatio.api.multisetindex.MultisetIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class RecipeSearchCoreTest {
    @Test
    void indexesRecipesThatShareKeysWithoutRecursiveSplitting() {
        DummyRecipe first = recipe("first", 10, 11);
        DummyRecipe second = recipe("second", 10, 12);
        DummyRecipe third = recipe("third", 10, 13);
        DummyRecipe fourth = recipe("fourth", 10, 14);
        MultisetIndex<DummyRecipe> index = build(List.of(first, second, third, fourth));

        assertSame(fourth, index.findAny(map(10, 14), recipe -> recipe.name.equals("fourth")));
    }

    @Test
    void findAnyStopsAfterTheFirstPredicateMatch() {
        DummyRecipe first = recipe("first", 60);
        DummyRecipe second = recipe("second", 60);
        DummyRecipe third = recipe("third", 60);
        MultisetIndex<DummyRecipe> index = build(List.of(first, second, third));
        AtomicInteger calls = new AtomicInteger();

        DummyRecipe result = index.findAny(map(60), candidate -> {
            calls.incrementAndGet();
            return true;
        });

        assertSame(first, result);
        assertEquals(1, calls.get());
    }

    @Test
    void fallsBackToRecipesWithoutIndexableKeys() {
        DummyRecipe indexed = recipe("indexed", 20);
        DummyRecipe fallback = recipe("fallback");
        MultisetIndex<DummyRecipe> index = build(List.of(indexed, fallback));

        assertSame(fallback, index.findAny(map(20), recipe -> recipe.name.equals("fallback")));
        assertEquals(1, index.indexedSize());
        assertEquals(1, index.fallbackSize());
    }

    @Test
    void fullSearchIncludesIndexedAndFallbackRecipes() {
        DummyRecipe indexed = recipe("indexed", 30);
        DummyRecipe fallback = recipe("fallback");
        MultisetIndex<DummyRecipe> index = build(List.of(indexed, fallback));

        List<String> results = index.findAll(map(30), recipe -> true).stream().map(recipe -> recipe.name).toList();

        assertEquals(List.of("indexed", "fallback"), results);
    }

    @Test
    void fullSearchReturnsEveryRecipeOnTheSameTreePath() {
        DummyRecipe first = recipe("first", 40);
        DummyRecipe second = recipe("second", 40);
        DummyRecipe third = recipe("third", 40);
        MultisetIndex<DummyRecipe> index = build(List.of(first, second, third));

        List<String> results = index.findAll(map(40), recipe -> true).stream().map(recipe -> recipe.name).toList();

        assertEquals(3, results.size());
        assertEquals(Set.of("first", "second", "third"), Set.copyOf(results));
    }

    @Test
    void preservesRequiredFeatureCounts() {
        DummyRecipe recipe = recipe("double", new int[]{50, 50});
        MultisetIndex<DummyRecipe> index = build(List.of(recipe));

        assertNull(index.findAny(map(50), candidate -> true));
        assertFalse(recipe.counts.matches(map(50)));
        assertSame(recipe, index.findAny(map(50, 50), candidate -> true));
        assertTrue(recipe.counts.matches(map(50, 50)));
    }

    @Test
    void sortsFeaturelessFallbackValuesWithConfiguredComparator() {
        DummyRecipe later = recipe("z-last");
        DummyRecipe earlier = recipe("a-first");
        MultisetIndex<DummyRecipe> index = MultisetIndex.build(
                List.of(later, earlier),
                recipe -> recipe.features,
                MultisetIndex.Options.<DummyRecipe>defaults()
                        .supportsParallel(recipe -> false)
                        .fallbackComparator(java.util.Comparator.comparing(recipe -> recipe.name)));

        assertSame(earlier, index.findAny(map(), recipe -> true));
    }

    @Test
    void searchesPathsDeeperThanOneMachineWord() {
        int[] keys = new int[70];
        for (int i = 0; i < keys.length; i++) keys[i] = 100 + i;
        DummyRecipe deep = recipe("deep", keys);
        MultisetIndex<DummyRecipe> index = build(List.of(deep));

        assertSame(deep, index.findAny(map(keys), recipe -> true));
    }

    @Test
    void findsAllValuesAcrossWideBranches() {
        List<DummyRecipe> recipes = List.of(
                recipe("one", 1), recipe("two", 2), recipe("three", 3),
                recipe("four", 4), recipe("five", 5));
        MultisetIndex<DummyRecipe> index = build(recipes);

        List<String> found = index.findAll(map(1, 2, 3, 4, 5), recipe -> true)
                .stream().map(recipe -> recipe.name).toList();

        assertEquals(Set.of("one", "two", "three", "four", "five"), Set.copyOf(found));
    }

    private static MultisetIndex<DummyRecipe> build(List<DummyRecipe> recipes) {
        return MultisetIndex.build(
                recipes,
                recipe -> recipe.features,
                MultisetIndex.Options.<DummyRecipe>defaults()
                        .supportsParallel(recipe -> false)
                        .onFeatureCountTable((recipe, counts) -> recipe.counts = counts));
    }

    private static DummyRecipe recipe(String name, int... keys) {
        return new DummyRecipe(name, map(keys));
    }

    private static FeatureMultiset map(int... keys) {
        FeatureMultiset features = new FeatureMultiset();
        for (int key : keys) features.add(key, 1);
        return features;
    }

    private static final class DummyRecipe {
        private final String name;
        private final FeatureMultiset features;
        private FeatureCounts counts;

        private DummyRecipe(String name, FeatureMultiset features) {
            this.name = name;
            this.features = features;
        }
    }
}
