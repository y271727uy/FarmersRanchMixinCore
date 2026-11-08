package com.y271727uy.FRMC.mixin.manors_bounty.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockPredicateFilter.class)
public interface BlockPredicateFilterInvoker {
    @Invoker("shouldPlace")
    boolean frmc$shouldPlace(PlacementContext context, RandomSource random, BlockPos pos);
}
