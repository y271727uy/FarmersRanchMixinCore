package com.y271727uy.FRMC.recipe.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecipeSearchCoreTest {
    @Test
    void indexesRecipesThatShareKeysWithoutRecursiveSplitting() {
        DummyRecipe first = recipe("first", 10, 11);
        DummyRecipe second = recipe("second", 10, 12);
        DummyRecipe third = recipe("third", 10, 13);
        DummyRecipe fourth = recipe("fourth", 10, 14);
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(first, second, third, fourth));

        IntLongMap input = map(10, 14);
        DummyRecipe result = database.findAnyMatch(input, input.toIntArray(), recipe -> recipe == fourth);

        assertSame(fourth, result);
    }

    @Test
    void fallsBackToRecipesWithoutIndexableKeys() {
        DummyRecipe indexed = recipe("indexed", 20);
        DummyRecipe fallback = recipe("fallback");
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(indexed, fallback));

        IntLongMap input = map(20);
        DummyRecipe result = database.findAnyMatch(input, input.toIntArray(), recipe -> recipe == fallback);

        assertSame(fallback, result);
    }

    @Test
    void fullSearchIncludesIndexedAndFallbackRecipes() {
        DummyRecipe indexed = recipe("indexed", 30);
        DummyRecipe fallback = recipe("fallback");
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(indexed, fallback));

        IntLongMap input = map(30);
        List<String> results = database.search(input, input.toIntArray(), recipe -> true)
                .stream()
                .map(recipe -> recipe.name)
                .toList();

        assertEquals(List.of("indexed", "fallback"), results);
    }

    @Test
    void fullSearchReturnsEveryRecipeOnTheSameTreePath() {
        DummyRecipe first = recipe("first", 40);
        DummyRecipe second = recipe("second", 40);
        DummyRecipe third = recipe("third", 40);
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(first, second, third));

        IntLongMap input = map(40);
        List<String> results = database.search(input, input.toIntArray(), recipe -> true)
                .stream()
                .map(recipe -> recipe.name)
                .toList();

        assertEquals(3, results.size());
        assertEquals(Set.of("first", "second", "third"), Set.copyOf(results));
    }

    @Test
    void recipeContainerPreservesRequiredItemCounts() {
        IntLongMap required = new IntLongMap();
        required.add(50, 2);
        DummyRecipe recipe = new DummyRecipe("double", required);
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(recipe));

        IntLongMap insufficient = map(50);
        assertNull(database.findAnyMatch(
                insufficient,
                insufficient.toIntArray(),
                candidate -> candidate.container.match(insufficient)
        ));

        IntLongMap sufficient = new IntLongMap();
        sufficient.add(50, 2);
        assertSame(recipe, database.findAnyMatch(
                sufficient,
                sufficient.toIntArray(),
                candidate -> candidate.container.match(sufficient)
        ));
    }

    @Test
    void searchesPathsDeeperThanOneMachineWord() {
        int[] keys = new int[70];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 100 + i;
        }
        DummyRecipe deep = recipe("deep", keys);
        DummyDB database = AbstractRecipeDB.build(new DummyDB(), List.of(deep));

        IntLongMap input = map(keys);
        assertSame(deep, database.findAnyMatch(input, input.toIntArray(), recipe -> true));
    }

    private static DummyRecipe recipe(String name, int... keys) {
        return new DummyRecipe(name, map(keys));
    }

    private static IntLongMap map(int... keys) {
        IntLongMap map = new IntLongMap();
        for (int key : keys) {
            map.add(key, 1L);
        }
        return map;
    }

    private static final class DummyDB extends AbstractRecipeDB<DummyRecipe> {
        @Override
        protected IntLongMap extractIntMap(DummyRecipe recipe) {
            return recipe.keys;
        }

        @Override
        protected void setRecipeContainer(DummyRecipe recipe, IntMapContainer container) {
            recipe.container = container;
        }

        @Override
        protected boolean supportsParallel(DummyRecipe recipe) {
            return false;
        }
    }

    private static final class DummyRecipe {
        private final String name;
        private final IntLongMap keys;
        private IntMapContainer container;

        private DummyRecipe(String name, IntLongMap keys) {
            this.name = name;
            this.keys = keys;
        }
    }
}
