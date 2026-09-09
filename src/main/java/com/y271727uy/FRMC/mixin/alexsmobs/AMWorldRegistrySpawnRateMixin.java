package com.y271727uy.FRMC.mixin.alexsmobs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.entity.manager.mobspawnactivity.SpawnRateReducer;
import com.y271727uy.FRMC.config.MobSpawnRateConfig;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Lowers Alex's Mobs natural-spawn weights while its biome list is assembled. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.world.AMWorldRegistry", remap = false)
public abstract class AMWorldRegistrySpawnRateMixin {
    @WrapOperation(method = "addBiomeSpawns", at = @At(value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), require = 0)
    private static boolean frmc$reduceNaturalSpawnWeight(List<Object> spawns, Object candidate,
                                                           Operation<Boolean> original) {
        if (candidate instanceof MobSpawnSettings.SpawnerData spawn) {
            return original.call(spawns, SpawnRateReducer.reduceWeight(spawn,
                    MobSpawnRateConfig.alexsMobsWeightMultiplier()));
        }
        return original.call(spawns, candidate);
    }
}
