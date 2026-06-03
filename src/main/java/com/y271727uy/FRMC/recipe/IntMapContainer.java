package com.y271727uy.FRMC.recipe;

/**
 * Container that holds an IntLongMap for a recipe, used for fast pre-filtering
 * during recipe matching. Stores the hash map extracted from a recipe's ingredients.
 */
public class IntMapContainer {
    final IntLongMap map;

    public IntMapContainer(IntLongMap map) {
        this.map = map;
    }

    /**
     * Returns true if this container's map is a subset of the given map,
     * meaning the input inventory contains at least the items required by this recipe.
     */
    public boolean match(IntLongMap inputMap) {
        return inputMap.containsAll(this.map);
    }
}
