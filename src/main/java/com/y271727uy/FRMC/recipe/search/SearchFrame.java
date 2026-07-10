/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import java.util.function.Consumer;

public final class SearchFrame<R> {
    boolean branchProbe;
    int index;
    BitContainer skip;
    private Branch<R> branch;

    void push(Branch<R> branch, int size, SearchFrame<R> previous) {
        this.branchProbe = size > branch.size();
        this.index = 0;
        this.skip = previous.branchProbe ? previous.skip : previous.skip.add(previous.index);
        this.branch = branch;
    }

    void push(Branch<R> branch, int size, int expectedDepth) {
        this.branchProbe = size > branch.size();
        this.index = 0;
        this.skip = expectedDepth > 64 ? BitContainer.MULTI_LONG : BitContainer.LONG;
        this.branch = branch;
    }

    R searchByInput(RecipeSearcher<R> searcher) {
        int[] inputs = searcher.ints;
        int size = inputs.length;
        int depth = searcher.depth;

        while (index < size) {
            if (skip.notContains(index)) {
                Node<R> node = branch.get(inputs[index]);
                if (node != null) {
                    R result = node.get(searcher, this);
                    if (result != null || searcher.depth != depth) {
                        index++;
                        return result;
                    }
                }
            }
            index++;
        }

        searcher.depth--;
        return null;
    }

    R searchByBranch(RecipeSearcher<R> searcher) {
        int[] keys = branch.key();
        Node<R>[] values = branch.value();
        int size = keys.length;
        int depth = searcher.depth;

        int currentIndex;
        while ((currentIndex = index++) < size) {
            if (searcher.map.containsKey(keys[currentIndex])) {
                R result = values[currentIndex].get(searcher, this);
                if (result != null || searcher.depth != depth) {
                    return result;
                }
            }
        }

        searcher.depth--;
        return null;
    }

    void forEachByInput(RecipeSearcher<R> searcher, Consumer<? super R> action) {
        int[] inputs = searcher.ints;
        int size = inputs.length;

        while (index < size) {
            if (skip.notContains(index)) {
                Node<R> node = branch.get(inputs[index]);
                if (node != null && node.forEach(searcher, this, action)) {
                    index++;
                    return;
                }
            }
            index++;
        }

        searcher.depth--;
    }

    void forEachByBranch(RecipeSearcher<R> searcher, Consumer<? super R> action) {
        int[] keys = branch.key();
        Node<R>[] values = branch.value();
        int size = keys.length;

        int currentIndex;
        while ((currentIndex = index++) < size) {
            if (searcher.map.containsKey(keys[currentIndex])
                    && values[currentIndex].forEach(searcher, this, action)) {
                return;
            }
        }

        searcher.depth--;
    }
}
