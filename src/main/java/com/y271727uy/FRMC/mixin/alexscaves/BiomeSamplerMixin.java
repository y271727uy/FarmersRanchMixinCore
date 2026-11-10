package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSampler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexscaves.ClientBiomeSampleCache;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BiomeSampler.class, remap = false)
public abstract class BiomeSamplerMixin {
    @WrapOperation(method = {"sampleBiomesFloatQuart", "sampleBiomesVec3Quart"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/BiomeManager;getNoiseBiomeAtQuart(III)Lnet/minecraft/core/Holder;",
            remap = true), require = 2)
    private static Holder<Biome> frmc$reuseSample(BiomeManager manager, int x, int y, int z,
                                                 Operation<Holder<Biome>> original) {
        return ClientBiomeSampleCache.sample(manager, x, y, z, original);
    }
}
