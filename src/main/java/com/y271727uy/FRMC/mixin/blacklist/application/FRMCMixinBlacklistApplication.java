package com.y271727uy.FRMC.mixin.blacklist.application;

import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.mixin.blacklist.FRMCMixinBlacklistConfig;
import com.y271727uy.FRMC.mixin.blacklist.FRMCMixinBlacklistExtension;
import com.y271727uy.FRMC.recipe.config.RecipeSearchConfig;

public final class FRMCMixinBlacklistApplication {
    private FRMCMixinBlacklistApplication() {
    }

    public static void initialize() {
        RecipeSearchConfig.load();
        FRMCMixinBlacklistConfig.load();
        FRMCMixinBlacklistExtension.register();
    }

    public static boolean shouldApplyMixin(String mixinClassName) {
        if (isRecipeMixin(mixinClassName)) {
            if (!Config.recipeSearchEnabled) {
                return false;
            }
            if (mixinClassName.contains(".recipe.sync.")) {
                return Config.recipeIngredientSyncEnabled;
            }
            if (mixinClassName.contains(".recipe.deduplicator.")) {
                return Config.recipeIngredientDeduplicatorEnabled;
            }
        }
        if (mixinClassName.contains(".farmersdelight.jei.")) {
            return isClassPresent("mezz.jei.api.recipe.category.IRecipeCategory");
        }
        return true;
    }

    private static boolean isRecipeMixin(String mixinClassName) {
        return mixinClassName.startsWith("com.y271727uy.FRMC.mixin.minecraft.recipe.");
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, FRMCMixinBlacklistApplication.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
