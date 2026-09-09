package com.y271727uy.FRMC.mixin.blacklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OptionalMixinLoadConditionTest {
    @Test
    void onlyAppliesExternalMixinWhenItsModIsLoaded() {
        assertFalse(FRMCMixinConfigPlugin.isOptionalIntegrationAvailable(
            "com.y271727uy.FRMC.mixin.mystiasizakaya.ItemFrameMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinConfigPlugin.isOptionalIntegrationAvailable(
            "com.y271727uy.FRMC.mixin.mystiasizakaya.ItemFrameMixin",
            "mystias_izakaya"::equals
        ));
    }

    @Test
    void keepsBaseMixinsEnabled() {
        assertTrue(FRMCMixinConfigPlugin.isOptionalIntegrationAvailable(
            "com.y271727uy.FRMC.mixin.minecraft.UtilMixin",
            modId -> false
        ));
    }

    @Test
    void recognizesFarmersDelightMixinBelowMinecraftPackage() {
        assertFalse(FRMCMixinConfigPlugin.isOptionalIntegrationAvailable(
            "com.y271727uy.FRMC.mixin.minecraft.recipe.deduplicator.farmersdelight.ToolActionIngredientSerializerMixin",
            modId -> false
        ));
    }
}
