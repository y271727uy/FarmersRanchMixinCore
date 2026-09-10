package com.y271727uy.FRMC.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FRMCMixinLoadConfigTest {
    @Test
    void onlyAppliesExternalMixinWhenItsModIsLoaded() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.mystiasizakaya.ItemFrameMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.mystiasizakaya.ItemFrameMixin",
            "mystias_izakaya"::equals
        ));
    }

    @Test
    void keepsBaseMixinsEnabled() {
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.minecraft.UtilMixin",
            modId -> false
        ));
    }

    @Test
    void recognizesFarmersDelightMixinBelowMinecraftPackage() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator.farmersdelight.ToolActionIngredientSerializerMixin",
            modId -> false
        ));
    }
}
