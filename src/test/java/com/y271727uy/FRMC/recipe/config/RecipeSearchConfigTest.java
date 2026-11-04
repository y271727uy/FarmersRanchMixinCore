package com.y271727uy.FRMC.recipe.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.config.Config;
import java.util.Properties;

import com.y271727uy.FRMC.config.RecipeSearchConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecipeSearchConfigTest {
    @AfterEach
    void restoreDefaults() {
        Config.recipeSearchEnabled = true;
        Config.recipeSearchOptimizeOnlyVanilla = false;
        Config.recipeSearchOptimizeOnlyVanillaRecipeClasses = false;
        Config.recipeIngredientSyncEnabled = false;
        Config.recipeIngredientDeduplicatorEnabled = false;
    }

    @Test
    void appliesGenericSearchDefaultsForMissingProperties() {
        RecipeSearchConfig.apply(new Properties());

        assertTrue(Config.recipeSearchEnabled);
        assertFalse(Config.recipeSearchOptimizeOnlyVanilla);
        assertFalse(Config.recipeSearchOptimizeOnlyVanillaRecipeClasses);
        assertFalse(Config.recipeIngredientSyncEnabled);
        assertFalse(Config.recipeIngredientDeduplicatorEnabled);
    }

}
