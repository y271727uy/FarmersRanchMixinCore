package com.y271727uy.FRMC.mixin.minecraft.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {
    @Accessor("targetPos")
    void frmc$setTargetPos(BlockPos target);

    @Accessor("reachRange")
    void frmc$setReachRange(int reachRange);

    @Invoker("resetStuckTimeout")
    void frmc$resetStuckTimeout();
}
