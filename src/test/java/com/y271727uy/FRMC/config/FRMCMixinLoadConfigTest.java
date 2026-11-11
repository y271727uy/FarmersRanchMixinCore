package com.y271727uy.FRMC.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FRMCMixinLoadConfigTest {
    @Test
    void keepsBothDependenciesForMovedCitadelRenderMixins() {
        String prefix = "com.y271727uy.FRMC.mixin.";
        for (String mixin : Set.of(
                "acceleratedrendering.buffer.AcceleratedRingBuffersCitadelMixin",
                "acceleratedrendering.render.LevelRendererCitadelMixin",
                "acceleratedrendering.render.LivingEntityRendererCitadelLodMixin")) {
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin, "citadel"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin, "acceleratedrendering"::equals));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin,
                    Set.of("citadel", "acceleratedrendering")::contains));
        }

        String boarLod = prefix + "acceleratedrendering.render.RendererBoarLodMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(boarLod,
                Set.of("citadel", "acceleratedrendering")::contains));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(boarLod,
                Set.of("citadel", "acceleratedrendering", "untamedwilds")::contains));
    }

    @Test
    void allowsSharedModelUploadMixinsWithoutCitadel() {
        String prefix = "com.y271727uy.FRMC.mixin.acceleratedrendering.";
        for (String mixin : Set.of("buffer.AcceleratedBufferBuilderCitadelMixin",
                "mesh.MeshUploadingDispatcherCitadelMixin", "mesh.ServerMeshBuilderCitadelMixin")) {
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin, "geckolib"::equals));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin,
                    Set.of("geckolib", "acceleratedrendering")::contains));
        }
    }

    @Test
    void allowsCitadelEmbeddiumFallbackWithoutAcceleratedRendering() {
        String prefix = "com.y271727uy.FRMC.mixin.citadel.model.";
        for (String mixin : Set.of("EmbeddiumAdvancedModelBoxMixin", "EmbeddiumBasicModelPartMixin")) {
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin, "citadel"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin, "embeddium"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin,
                    Set.of("citadel", "acceleratedrendering")::contains));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(prefix + mixin,
                    Set.of("citadel", "embeddium")::contains));
        }
    }

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

    @Test
    void mapsWorldMapAndMinimapMixinsIndependently() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.xaero.worldmap.GuiMapMixin",
            modId -> "xaerominimap".equals(modId)
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.xaero.worldmap.GuiMapMixin",
            "xaeroworldmap"::equals
        ));
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.xaero.minimap.MinimapRendererMixin",
            modId -> "xaeroworldmap".equals(modId)
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.xaero.minimap.MinimapRendererMixin",
            "xaerominimap"::equals
        ));
    }

    @Test
    void skipsBetterFoliageMixinWhenBetterFoliageIsAbsent() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.betterfoliage.LeavesBakedModelMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.betterfoliage.LeavesBakedModelMixin",
            "betterfoliage"::equals
        ));
    }

    @Test
    void skipsKubejsFenceWhenKubejsIsAbsent() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kubejs.ScriptManagerMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kubejs.ScriptManagerMixin",
            "kubejs"::equals
        ));
    }

    @Test
    void skipsSereneSeasonsOptimizerWhenSereneSeasonsIsAbsent() {
        String mixin = "com.y271727uy.FRMC.mixin.sereneseasons.ServerLevelMeltMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "sereneseasons"::equals));
    }

    @Test
    void skipsYoukaiHomecomingMixinWhenModIsAbsent() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.youkaishomecoming.TableItemManagerMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.youkaishomecoming.TableItemManagerMixin",
            "youkaishomecoming"::equals
        ));
    }

    @Test
    void skipsKaleidoscopeTavernMixinWhenModIsAbsent() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kaleidoscopetavern.BarCabinetBlockMixin",
            modId -> false
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kaleidoscopetavern.BarCabinetBlockMixin",
            "kaleidoscope_tavern"::equals
        ));
    }

    @Test
    void appliesSdvfMixinWhenSdvfIsLoaded() {
        String mixin = "com.y271727uy.FRMC.mixin.stardew_valley_food.SdvfModMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "sdvf"::equals));
    }

    @Test
    void appliesGeckolibMixinWhenGeckolibIsLoaded() {
        String mixin = "com.y271727uy.FRMC.mixin.geckolib.renderer.GeoRendererMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "geckolib"::equals));
    }

    @Test
    void suppliesGeckoAccelerationOnlyWhenUpstreamMixinsAreGatedOff() {
        for (String name : Set.of("GeoBoneAccelerationMixin", "GeoRendererAccelerationMixin")) {
            String mixin = "com.y271727uy.FRMC.mixin.acceleratedrendering.geckolib." + name;
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "geckolib"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "acceleratedrendering"::equals));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "geckolib")::contains));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "geckolib", "mixinbooster")::contains));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "geckolib", "connectormod")::contains));
        }
    }

    @Test
    void suppliesMaidAccelerationIndependentlyOfGeckoLib4() {
        for (String name : Set.of("GeoBoneAccelerationMixin", "SodiumCompatMixin")) {
            String mixin = "com.y271727uy.FRMC.mixin.acceleratedrendering.touhoulittlemaid." + name;
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "touhou_little_maid"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "geckolib")::contains));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "touhou_little_maid")::contains));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "touhou_little_maid", "mixinbooster")::contains));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin,
                Set.of("acceleratedrendering", "touhou_little_maid", "connectormod")::contains));
        }
    }

    @Test
    void keepsUnconditionalServerPlayerMixinEnabled() {
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.minecraft.server.ServerPlayerMixin",
            modId -> false
        ));
    }

    @Test
    void requiresBothModsForWineCabinetFakeModelMixins() {
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kaleidoscopetavern.vinery.BottleBlockFakeModelMixin",
            "kaleidoscope_tavern"::equals
        ));
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kaleidoscopetavern.vinery.BottleBlockFakeModelMixin",
            "vinery"::equals
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.kaleidoscopetavern.vinery.BottleBlockFakeModelMixin",
            Set.of("kaleidoscope_tavern", "vinery")::contains
        ));
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.youkaishomecoming.vinery.BottleBlockFakeModelMixin",
            "youkaishomecoming"::equals
        ));
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.youkaishomecoming.vinery.BottleBlockFakeModelMixin",
            "vinery"::equals
        ));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            "com.y271727uy.FRMC.mixin.youkaishomecoming.vinery.BottleBlockFakeModelMixin",
            Set.of("youkaishomecoming", "vinery")::contains
        ));
    }

    @Test
    void skipsEggDelightMixinsWhenEggDelightIsAbsent() {
        String mixin = "com.y271727uy.FRMC.mixin.eggdelight.RawOmeletUpdateTickProcedureMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "eggdelight"::equals));
    }
    @Test
    void skipsVietnamDelightWormMixinWithoutTheMcreatorClass() {
        String mixin = "com.y271727uy.FRMC.mixin.vietnam_delight.WormDropHandlerMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        // 1.1.3 keeps the mod id but deleted WormDropHandler. The test classpath is that jar.
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "vietnam_delight"::equals));
    }

    @Test
    void loadsTerraBlenderCacheMixinOnlyWhenTheModIsPresent() {
        String mixin = "com.y271727uy.FRMC.mixin.terrablender.AreaCacheMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "terrablender"::equals));
    }

    @Test
    void requiresRuokAndParticleRainForWeatherCullBypass() {
        String mixin = "com.y271727uy.FRMC.mixin.ruok.particlerain.ParticleRenderMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "ruokmod"::equals));
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "particlerain"::equals));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
            mixin, Set.of("ruokmod", "particlerain")::contains
        ));
    }

    @Test
    void loadsFastChunkGenCompatibilityMixinOnlyWhenModIsPresent() {
        String mixin = "com.y271727uy.FRMC.mixin.fastchunkgen.TypeMixin";
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
    }

    @Test
    void loadsAsyncParticlesThreadSafetyMixinsOnlyWithAllThreeMods() {
        String animateMixin = "com.y271727uy.FRMC.mixin.asyncparticles.AnimateTickThreadSafetyMixin";
        String rainMixin = "com.y271727uy.FRMC.mixin.asyncparticles.PrettyRainThreadSafetyMixin";
        for (String mixin : Set.of(animateMixin, rainMixin)) {
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "asyncparticles"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "fastchunkgen"::equals));
            assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "particlerain"::equals));
            assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(
                mixin, Set.of("asyncparticles", "fastchunkgen", "particlerain")::contains
            ));
        }
    }

    @Test
    void loadsOverweightFarmingMixinOnlyWhenModIsPresent() {
        String mixin = "com.y271727uy.FRMC.mixin.overweight_farming.FarmBlockMixin";
        assertFalse(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, modId -> false));
        assertTrue(FRMCMixinLoadConfig.isIntegrationEnabled(mixin, "overweight_farming"::equals));
    }
}
