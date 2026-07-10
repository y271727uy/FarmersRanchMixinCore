package com.y271727uy.FRMC.recipe.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.Config;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecipeSearchConfigTest {
    @AfterEach
    void restoreDefaults() {
        Config.recipeSearchEnabled = true;
        Config.recipeSearchOptimizeOnlyVanilla = true;
        Config.recipeIngredientSyncEnabled = false;
        Config.recipeIngredientDeduplicatorEnabled = false;
    }

    @Test
    void appliesUpstreamCompatibleDefaultsForMissingProperties() {
        RecipeSearchConfig.apply(new Properties());

        assertTrue(Config.recipeSearchEnabled);
        assertTrue(Config.recipeSearchOptimizeOnlyVanilla);
        assertFalse(Config.recipeIngredientSyncEnabled);
        assertFalse(Config.recipeIngredientDeduplicatorEnabled);
    }

}
