package com.y271727uy.FRMC.mixin.sereneseasons;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapInvokerMixin {
    @Invoker("anyPlayerCloseEnoughForSpawning")
    boolean frmc$anyPlayerCloseEnoughForSpawning(ChunkPos chunkPos);
}
