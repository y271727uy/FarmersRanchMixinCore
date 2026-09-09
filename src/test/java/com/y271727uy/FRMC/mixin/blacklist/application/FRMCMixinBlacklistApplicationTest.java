package com.y271727uy.FRMC.mixin.blacklist.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FRMCMixinBlacklistApplicationTest {
    @AfterEach
    void restoreDefaults() {
        Config.recipeSearchEnabled = true;
        Config.recipeIngredientSyncEnabled = false;
        Config.recipeIngredientDeduplicatorEnabled = false;
    }

    @Test
    void honorsRecipeFeatureFlags() {
        assertTrue(FRMCMixinBlacklistApplication.shouldApplyMixin(
                "com.y271727uy.FRMC.mixin.minecraft.recipe.manager.ServerResourcesMixin"
        ));
        assertFalse(FRMCMixinBlacklistApplication.shouldApplyMixin(
                "com.y271727uy.FRMC.mixin.minecraft.recipe.sync.IngredientMixin"
        ));
        assertFalse(FRMCMixinBlacklistApplication.shouldApplyMixin(
                "com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator.ItemMixin"
        ));

        Config.recipeSearchEnabled = false;
        assertFalse(FRMCMixinBlacklistApplication.shouldApplyMixin(
                "com.y271727uy.FRMC.mixin.minecraft.recipe.manager.ServerResourcesMixin"
        ));
    }
}
