package com.y271727uy.FRMC.mixin.minecraft.villager.diagnostics;

import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the villager profiler name the children of RunOne and other composite behaviors. Read-only. */
@Mixin(GateBehavior.class)
public interface GateBehaviorAccessor {
    @Accessor("behaviors")
    ShufflingList<?> frmc$children();
}
