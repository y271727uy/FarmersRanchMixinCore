package com.y271727uy.FRMC.mixin.unusualfishmod;

import com.y271727uy.FRMC.entity.manager.mobspawnactivity.SpawnRateReducer;
import com.y271727uy.FRMC.config.MobSpawnRateConfig;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scales only Unusual Fish entries submitted through Forge's standard add-spawns modifier. */
@Pseudo
@Mixin(MobSpawnSettings.Builder.class)
public abstract class UnusualFishSpawnRateMixin {
    @ModifyVariable(method = "addSpawn", at = @At("HEAD"), argsOnly = true)
    private MobSpawnSettings.SpawnerData frmc$reduceUnusualFishSpawnWeight(MobSpawnSettings.SpawnerData spawn) {
        return spawn.type.builtInRegistryHolder().key().location().getNamespace().equals("unusualfishmod")
                ? SpawnRateReducer.reduceWeight(spawn, MobSpawnRateConfig.unusualFishWeightMultiplier())
                : spawn;
    }
}
