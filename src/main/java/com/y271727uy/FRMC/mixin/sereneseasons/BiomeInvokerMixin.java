package com.y271727uy.FRMC.mixin.sereneseasons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Biome.class)
public interface BiomeInvokerMixin {
    @Invoker("getTemperature")
    float frmc$getTemperature(BlockPos pos);
}
