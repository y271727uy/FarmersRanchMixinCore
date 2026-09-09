package com.y271727uy.FRMC.mixin.aquaculture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.entity.manager.mobspawnactivity.SpawnRateReducer;
import com.y271727uy.FRMC.config.MobSpawnRateConfig;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Scales Aquaculture's biome-modifier spawn weights before they reach the biome builder. */
@Pseudo
@Mixin(targets = "com.teammetallurgy.aquaculture.loot.AquaBiomeModifiers$FishSpawnBiomeModifier", remap = false)
public abstract class AquacultureFishSpawnRateMixin {
    @WrapOperation(method = "modify", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/world/MobSpawnSettingsBuilder;addSpawn(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;)Lnet/minecraft/world/level/biome/MobSpawnSettings$Builder;"), require = 0)
    private MobSpawnSettings.Builder frmc$reduceNaturalSpawnWeight(MobSpawnSettingsBuilder builder,
                                                                     MobCategory category,
                                                                     MobSpawnSettings.SpawnerData spawn,
                                                                     Operation<MobSpawnSettings.Builder> original) {
        return original.call(builder, category, SpawnRateReducer.reduceWeight(spawn,
                MobSpawnRateConfig.aquacultureWeightMultiplier()));
    }
}
