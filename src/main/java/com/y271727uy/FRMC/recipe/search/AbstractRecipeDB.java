/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class AbstractRecipeDB<R> {
    protected Branch<R> rootBranch;
    protected List<R> serialRecipes;
    protected List<R> parallelRecipes;
    protected int maxSearchDepth;
    protected int minParallelThreshold = 100;

    public static <R, DB extends AbstractRecipeDB<R>> DB build(DB database, Collection<R> recipes) {
        database.clear();
        List<Runnable> branchConstructionTasks = new ArrayList<>(recipes.size());
        Map<R, Pair<IntLongMap, IntMapContainer>> recipeContainers =
                new Reference2ReferenceOpenHashMap<>(recipes.size());
        Int2IntOpenHashMap frequencyMap = new Int2IntOpenHashMap();

        branchConstructionTasks.add(() -> database.rootBranch = database.rootBranch.optimize());
        recipes.forEach(recipe -> database.collectRecipeData(frequencyMap, recipeContainers, recipe));
        recipeContainers.forEach((recipe, pair) -> database.reorderRecipeByFrequency(frequencyMap, pair));
        recipeContainers.forEach((recipe, pair) -> database.addToBranch(branchConstructionTasks, recipe, pair.right()));
        branchConstructionTasks.forEach(Runnable::run);
        database.finishBuild();
        return database;
    }

    protected abstract IntLongMap extractIntMap(R recipe);

    protected abstract void setRecipeContainer(R recipe, IntMapContainer container);

    protected boolean supportsParallel(R recipe) {
        return true;
    }

    public R findAnyMatch(IntLongMap map, int[] searchKeys, Predicate<R> predicate) {
        R foundRecipe = new RecipeSearcher<>(
                maxSearchDepth, rootBranch, map, searchKeys, predicate, null
        ).findAny();
        if (foundRecipe != null) {
            return foundRecipe;
        }
        if (!parallelRecipes.isEmpty()) {
            foundRecipe = findInParallel(parallelRecipes, predicate);
            if (foundRecipe != null) {
                return foundRecipe;
            }
        }
        return serialRecipes.isEmpty() ? null : findInSerial(serialRecipes, predicate);
    }

    protected static <R> R findInParallel(List<R> recipes, Predicate<R> predicate) {
        return recipes.parallelStream().filter(predicate).findAny().orElse(null);
    }

    protected static <R> R findInSerial(List<R> recipes, Predicate<R> predicate) {
        for (R recipe : recipes) {
            if (predicate.test(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    public Iterator<R> createFallbackIterator(Predicate<R> predicate) {
        Iterator<R> parallelIterator = parallelRecipes.isEmpty()
                ? null
                : parallelRecipes.parallelStream().filter(predicate).iterator();
        Iterator<R> serialIterator = serialRecipes.isEmpty()
                ? null
                : IteratorUtil.filter(serialRecipes.iterator(), predicate);
        if (parallelIterator == null) {
            return serialIterator;
        }
        return serialIterator == null
                ? parallelIterator
                : IteratorUtil.concat(parallelIterator, serialIterator);
    }

    public Iterable<R> searchFallback(Predicate<R> predicate) {
        Iterator<R> iterator = createFallbackIterator(predicate);
        return iterator == null ? Collections.emptyList() : IteratorUtil.wrap(iterator);
    }

    public RecipeSearcher<R> search(IntLongMap map, int[] searchKeys, Predicate<R> predicate) {
        return new RecipeSearcher<>(
                maxSearchDepth,
                rootBranch,
                map,
                searchKeys,
                predicate,
                createFallbackIterator(predicate)
        );
    }

    protected void finishBuild() {
        if (parallelRecipes.size() < minParallelThreshold) {
            serialRecipes.addAll(parallelRecipes);
            parallelRecipes = Collections.emptyList();
        }
        if (serialRecipes.isEmpty()) {
            serialRecipes = Collections.emptyList();
        }
    }

    protected void addToBranch(List<Runnable> branchConstructionTasks, R recipe, IntMapContainer container) {
        int[] keys = container.key;
        int searchDepth = keys.length;
        maxSearchDepth = Math.max(maxSearchDepth, searchDepth);
        addToBranch(branchConstructionTasks, recipe, searchDepth, keys, rootBranch);
    }

    @SuppressWarnings("unchecked")
    protected void addToBranch(
            List<Runnable> branchConstructionTasks,
            R recipe,
            int depth,
            int[] keys,
            Branch<R> branch
    ) {
        Branch<R> currentBranch = branch;
        int lastIndex = depth - 1;
        for (int i = 0; i < depth; i++) {
            boolean intermediate = i < lastIndex;
            Node<R> node = ((Branch.HashBranch<R>) currentBranch).compute(
                    keys[i],
                    (key, existingNode) -> intermediate
                            ? Node.branch(branchConstructionTasks, existingNode)
                            : Node.recipe(branchConstructionTasks, existingNode, recipe)
            );
            if (intermediate) {
                currentBranch = ((Node.BranchNode<R>) node).branch();
            }
        }
    }

    protected void collectRecipeData(
            Int2IntOpenHashMap frequencyMap,
            Map<R, Pair<IntLongMap, IntMapContainer>> recipeContainers,
            R recipe
    ) {
        if (recipe == null) {
            return;
        }
        IntLongMap intMap = extractIntMap(recipe);
        int[] keys = intMap.toIntArray();
        if (keys.length == 0) {
            if (supportsParallel(recipe)) {
                parallelRecipes.add(recipe);
            } else {
                serialRecipes.add(recipe);
            }
            return;
        }

        for (int key : keys) {
            frequencyMap.addTo(key, 1);
        }
        IntMapContainer container = new IntMapContainer(keys);
        setRecipeContainer(recipe, container);
        recipeContainers.put(recipe, Pair.of(intMap, container));
    }

    protected void reorderRecipeByFrequency(
            Int2IntOpenHashMap frequencyMap,
            Pair<IntLongMap, IntMapContainer> pair
    ) {
        IntLongMap intMap = pair.left();
        IntMapContainer container = pair.right();
        int[] keys = container.key;
        IntArrays.stableSort(keys, (a, b) -> Integer.compare(frequencyMap.get(a), frequencyMap.get(b)));
        long[] values = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = intMap.get(keys[i]);
        }
        container.value = values;
    }

    public void clear() {
        rootBranch = Branch.create();
        serialRecipes = new ArrayList<>();
        parallelRecipes = new ArrayList<>();
        maxSearchDepth = 0;
    }
}
