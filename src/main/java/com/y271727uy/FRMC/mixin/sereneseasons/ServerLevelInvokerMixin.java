package com.y271727uy.FRMC.mixin.sereneseasons;

import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerLevel.class)
public interface ServerLevelInvokerMixin {
    @Invoker("isPositionTickingWithEntitiesLoaded")
    boolean frmc$isPositionTickingWithEntitiesLoaded(long chunkPos);
}
