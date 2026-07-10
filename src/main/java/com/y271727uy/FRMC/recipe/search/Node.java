/*
 * Copyright (c) 2025 nutant233
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.y271727uy.FRMC.recipe.search;

import java.util.List;
import java.util.function.Consumer;

public interface Node<T> {
    @SuppressWarnings("unchecked")
    static <T> Node<T> recipe(List<Runnable> branchBuilder, Node<T> node, T value) {
        if (node instanceof BR<T> branchRecipe) {
            T[] recipes = (T[]) new Object[]{branchRecipe.recipe, value};
            return new BMR<>(branchBuilder, branchRecipe.branch, recipes);
        }
        if (node instanceof BMR<T> branchRecipes) {
            T[] recipes = (T[]) new Object[branchRecipes.recipes.length + 1];
            System.arraycopy(branchRecipes.recipes, 0, recipes, 0, branchRecipes.recipes.length);
            recipes[branchRecipes.recipes.length] = value;
            return new BMR<>(branchBuilder, branchRecipes.branch, recipes);
        }
        if (node instanceof R<T> recipe) {
            T[] recipes = (T[]) new Object[]{recipe.recipe, value};
            return new MR<>(recipes);
        }
        if (node instanceof MR<T> multipleRecipes) {
            T[] recipes = (T[]) new Object[multipleRecipes.recipes.length + 1];
            System.arraycopy(multipleRecipes.recipes, 0, recipes, 0, multipleRecipes.recipes.length);
            recipes[multipleRecipes.recipes.length] = value;
            return new MR<>(recipes);
        }
        if (node instanceof B<T> branch) {
            return new BR<>(branchBuilder, branch.branch, value);
        }
        return new R<>(value);
    }

    static <T> Node<T> branch(List<Runnable> branchBuilder, Node<T> node) {
        if (node instanceof B<?> || node instanceof BR<?> || node instanceof BMR<?>) {
            return node;
        }
        if (node instanceof R<T> recipe) {
            return new BR<>(branchBuilder, Branch.create(), recipe.recipe);
        }
        if (node instanceof MR<T> recipes) {
            return new BMR<>(branchBuilder, Branch.create(), recipes.recipes);
        }
        return new B<>(branchBuilder);
    }

    default T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
        return null;
    }

    default boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
        return false;
    }

    final class B<T> implements BranchNode<T> {
        private Branch<T> branch = Branch.create();

        private B(List<Runnable> branchBuilder) {
            branchBuilder.add(() -> branch = branch.optimize());
        }

        @Override
        public Branch<T> branch() {
            return branch;
        }
    }

    final class BMR<T> extends MR<T> implements BranchNode<T> {
        private Branch<T> branch;

        private BMR(List<Runnable> branchBuilder, Branch<T> branch, T[] recipes) {
            super(recipes);
            this.branch = branch;
            branchBuilder.add(() -> this.branch = this.branch.optimize());
        }

        @Override
        public Branch<T> branch() {
            return branch;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            if (frame != null) {
                BranchNode.super.get(context, frame);
            }
            return super.get(context, frame);
        }

        @Override
        public boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
            BranchNode.super.forEach(context, frame, action);
            for (T recipe : recipes) {
                if (context.predicate.test(recipe)) {
                    action.accept(recipe);
                }
            }
            return true;
        }
    }

    final class BR<T> extends R<T> implements BranchNode<T> {
        private Branch<T> branch;

        private BR(List<Runnable> branchBuilder, Branch<T> branch, T recipe) {
            super(recipe);
            this.branch = branch;
            branchBuilder.add(() -> this.branch = this.branch.optimize());
        }

        @Override
        public Branch<T> branch() {
            return branch;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            BranchNode.super.get(context, frame);
            return context.predicate.test(recipe) ? recipe : null;
        }

        @Override
        public boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
            BranchNode.super.forEach(context, frame, action);
            if (context.predicate.test(recipe)) {
                action.accept(recipe);
            }
            return true;
        }
    }

    interface BranchNode<T> extends Node<T> {
        Branch<T> branch();

        @Override
        default T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            int depth = ++context.depth;
            if (depth == context.maxDepth) {
                context.expansion();
            }
            context.frames[depth].push(branch(), context.ints.length - depth, frame);
            return null;
        }

        @Override
        default boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
            int depth = ++context.depth;
            if (depth == context.maxDepth) {
                context.expansion();
            }
            context.frames[depth].push(branch(), context.ints.length - depth, frame);
            return true;
        }
    }

    class MR<T> implements Node<T> {
        final T[] recipes;
        private final int length;

        MR(T[] recipes) {
            this.recipes = recipes;
            this.length = recipes.length;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            int index;
            while ((index = context.count++) < length) {
                T recipe = recipes[index];
                if (context.predicate.test(recipe)) {
                    if (index < length - 1) {
                        context.node = this;
                    }
                    return recipe;
                }
            }
            context.count = 0;
            context.node = null;
            return null;
        }

        @Override
        public boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
            for (T recipe : recipes) {
                if (context.predicate.test(recipe)) {
                    action.accept(recipe);
                }
            }
            return false;
        }
    }

    class R<T> implements Node<T> {
        final T recipe;

        R(T recipe) {
            this.recipe = recipe;
        }

        @Override
        public T get(RecipeSearcher<T> context, SearchFrame<T> frame) {
            return context.predicate.test(recipe) ? recipe : null;
        }

        @Override
        public boolean forEach(RecipeSearcher<T> context, SearchFrame<T> frame, Consumer<? super T> action) {
            if (context.predicate.test(recipe)) {
                action.accept(recipe);
            }
            return false;
        }
    }
}
