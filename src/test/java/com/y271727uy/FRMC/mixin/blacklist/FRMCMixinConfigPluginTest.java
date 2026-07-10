package com.y271727uy.FRMC.mixin.blacklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FRMCMixinConfigPluginTest {
    @AfterEach
    void restoreDefaults() {
        Config.recipeSearchEnabled = true;
        Config.recipeIngredientSyncEnabled = false;
        Config.recipeIngredientDeduplicatorEnabled = false;
    }

    @Test
    void honorsRecipeFeatureFlags() {
        FRMCMixinConfigPlugin plugin = new FRMCMixinConfigPlugin();

        assertTrue(plugin.shouldApplyMixin(
                "ignored",
                "com.y271727uy.FRMC.mixin.minecraft.recipe.manager.ServerResourcesMixin"
        ));
        assertFalse(plugin.shouldApplyMixin(
                "ignored",
                "com.y271727uy.FRMC.mixin.minecraft.recipe.sync.IngredientMixin"
        ));
        assertFalse(plugin.shouldApplyMixin(
                "ignored",
                "com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator.ItemMixin"
        ));

        Config.recipeSearchEnabled = false;
        assertFalse(plugin.shouldApplyMixin(
                "ignored",
                "com.y271727uy.FRMC.mixin.minecraft.recipe.manager.ServerResourcesMixin"
        ));
    }
}
