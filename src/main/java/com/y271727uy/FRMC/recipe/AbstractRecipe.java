package com.y271727uy.FRMC.recipe;

import java.util.*;
import java.util.function.Predicate;

/**
 * Abstract base class for a recipe database that uses a decision tree
 * to accelerate recipe lookups.
 * <p>
 * The algorithm:
 * 1. Extract an IntLongMap from each recipe (mapping item/tag hash → count)
 * 2. Build a binary decision tree where each node splits recipes based on
 *    whether they contain a particular hash
 * 3. At query time, extract the IntLongMap from the input inventory,
 *    then traverse the tree to find candidate recipes
 * 4. Only the candidate recipes are checked with the full matches() call
 *
 * @param <H> the recipe holder type
 */
public abstract class AbstractRecipe<H> {

    /** Root branch of the decision tree, or null if no tree was built */
    protected BranchNode<H> rootBranch;

    /** Recipes that couldn't be placed in the tree (fallback to linear search) */
    protected List<H> serialRecipes = new ArrayList<>();

    /**
     * Build a recipe database from a list of recipe holders.
     */
    public static <H> AbstractRecipe<H> build(AbstractRecipe<H> db, List<H> recipes) {
        // Phase 1: Extract IntLongMap from each recipe
        List<RecipeEntry<H>> entries = new ArrayList<>();
        for (H recipe : recipes) {
            if (db.supportsParallel(recipe)) {
                IntLongMap map = db.extractIntMap(recipe);
                if (!map.isEmpty()) {
                    IntMapContainer container = new IntMapContainer(cloneMap(map));
                    db.setRecipeContainer(recipe, container);
                    entries.add(new RecipeEntry<>(recipe, map));
                } else {
                    db.serialRecipes.add(recipe);
                }
            } else {
                db.serialRecipes.add(recipe);
            }
        }

        // Phase 2: Build decision tree if we have enough recipes
        if (!entries.isEmpty()) {
            db.rootBranch = buildTree(entries);
        }

        // Phase 3: Post-build callback
        db.finishBuild();

        return db;
    }

    /**
     * Build a decision tree from recipe entries.
     * Each node picks the hash key that appears in the most recipes,
     * then splits recipes into "contains key" and "doesn't contain key" groups.
     */
    private static <H> BranchNode<H> buildTree(List<RecipeEntry<H>> entries) {
        if (entries.isEmpty()) return null;
        if (entries.size() <= 2) {
            // Small enough to be a leaf
            BranchNode<H> node = new BranchNode<>();
            node.recipes = entries.stream().map(e -> e.holder).toList();
            return node;
        }

        // Find the most common hash key among all recipes
        Map<Integer, Integer> frequency = new HashMap<>();
        for (RecipeEntry<H> entry : entries) {
            IntLongMap map = entry.map;
            int[] keys = map.toIntArray();
            for (int key : keys) {
                frequency.merge(key, 1, Integer::sum);
            }
        }

        if (frequency.isEmpty()) {
            BranchNode<H> node = new BranchNode<>();
            node.recipes = entries.stream().map(e -> e.holder).toList();
            return node;
        }

        // Pick the hash key that appears in the most recipes (best discriminator)
        int bestKey = Collections.max(frequency.entrySet(), Map.Entry.comparingByValue()).getKey();

        // Split recipes into those that contain bestKey and those that don't
        List<RecipeEntry<H>> contains = new ArrayList<>();
        List<RecipeEntry<H>> notContains = new ArrayList<>();

        for (RecipeEntry<H> entry : entries) {
            if (entry.map.containsKey(bestKey)) {
                contains.add(entry);
            } else {
                notContains.add(entry);
            }
        }

        BranchNode<H> node = new BranchNode<>();
        node.splitKey = bestKey;

        if (!contains.isEmpty()) {
            node.matchBranch = buildTree(contains);
        }
        if (!notContains.isEmpty()) {
            node.noMatchBranch = buildTree(notContains);
        }

        return node;
    }

    private static IntLongMap cloneMap(IntLongMap map) {
        IntLongMap clone = new IntLongMap();
        int[] keys = map.toIntArray();
        for (int key : keys) {
            clone.add(key, map.get(key));
        }
        return clone;
    }

    /**
     * Find any single recipe that matches the given input.
     */
    protected H findAnyMatch(IntLongMap inputMap, int[] inputKeys, Predicate<H> predicate) {
        if (rootBranch == null) return null;
        return findAnyMatchRecursive(rootBranch, inputMap, inputKeys, predicate);
    }

    private H findAnyMatchRecursive(BranchNode<H> node, IntLongMap inputMap, int[] inputKeys, Predicate<H> predicate) {
        if (node == null) return null;

        // Leaf node: check all recipes linearly
        if (node.splitKey == 0 && node.recipes != null) {
            for (H recipe : node.recipes) {
                if (predicate.test(recipe)) {
                    return recipe;
                }
            }
            return null;
        }

        // Check if input contains the split key
        boolean hasKey = inputMap.containsKey(node.splitKey);

        // Try the matching branch first
        if (hasKey && node.matchBranch != null) {
            H result = findAnyMatchRecursive(node.matchBranch, inputMap, inputKeys, predicate);
            if (result != null) return result;
        }

        // Try the non-matching branch
        if (!hasKey && node.noMatchBranch != null) {
            H result = findAnyMatchRecursive(node.noMatchBranch, inputMap, inputKeys, predicate);
            if (result != null) return result;
        }

        // If the preferred branch didn't work, try the other one
        if (hasKey && node.noMatchBranch != null) {
            return findAnyMatchRecursive(node.noMatchBranch, inputMap, inputKeys, predicate);
        } else if (!hasKey && node.matchBranch != null) {
            return findAnyMatchRecursive(node.matchBranch, inputMap, inputKeys, predicate);
        }

        return null;
    }

    /**
     * Find all recipes that match the given input.
     */
    protected List<H> search(IntLongMap inputMap, int[] inputKeys, Predicate<H> predicate) {
        List<H> results = new ArrayList<>();
        if (rootBranch != null) {
            searchRecursive(rootBranch, inputMap, inputKeys, predicate, results);
        }
        return results;
    }

    private void searchRecursive(BranchNode<H> node, IntLongMap inputMap, int[] inputKeys, Predicate<H> predicate, List<H> results) {
        if (node == null) return;

        // Leaf node: check all recipes
        if (node.splitKey == 0 && node.recipes != null) {
            for (H recipe : node.recipes) {
                if (predicate.test(recipe)) {
                    results.add(recipe);
                }
            }
            return;
        }

        boolean hasKey = inputMap.containsKey(node.splitKey);

        if (hasKey && node.matchBranch != null) {
            searchRecursive(node.matchBranch, inputMap, inputKeys, predicate, results);
        }
        if (!hasKey && node.noMatchBranch != null) {
            searchRecursive(node.noMatchBranch, inputMap, inputKeys, predicate, results);
        }

        // Also search the other branch if the split key is present in input
        // (a recipe might not need this particular key but still match)
        if (hasKey && node.noMatchBranch != null) {
            searchRecursive(node.noMatchBranch, inputMap, inputKeys, predicate, results);
        }
    }

    /**
     * Linear search fallback for recipes not in the tree.
     */
    protected H findInSerial(List<H> list, Predicate<H> predicate) {
        for (H recipe : list) {
            if (predicate.test(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Called after the tree is built. Subclasses can override to perform
     * post-processing like sorting serial recipes or optimizing data structures.
     */
    protected void finishBuild() {
    }

    /**
     * Returns true if this recipe can be placed in the decision tree.
     */
    protected abstract boolean supportsParallel(H recipe);

    /**
     * Extract an IntLongMap from a recipe, mapping item/tag hashes to their counts.
     */
    protected abstract IntLongMap extractIntMap(H recipe);

    /**
     * Store the IntMapContainer on the recipe holder for fast pre-filtering.
     */
    protected abstract void setRecipeContainer(H holder, IntMapContainer container);

    /**
     * A node in the binary decision tree.
     */
    protected static class BranchNode<H> {
        /** The hash key to split on. 0 means this is a leaf node. */
        int splitKey;
        /** Branch for recipes that contain splitKey */
        BranchNode<H> matchBranch;
        /** Branch for recipes that don't contain splitKey */
        BranchNode<H> noMatchBranch;
        /** Recipes at this node (only for leaf nodes) */
        List<H> recipes;
    }

    /**
     * Internal entry pairing a recipe holder with its extracted IntLongMap.
     */
    private static class RecipeEntry<H> {
        final H holder;
        final IntLongMap map;

        RecipeEntry(H holder, IntLongMap map) {
            this.holder = holder;
            this.map = map;
        }
    }
}
