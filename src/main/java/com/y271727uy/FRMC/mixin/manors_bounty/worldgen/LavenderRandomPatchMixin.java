package com.y271727uy.FRMC.mixin.manors_bounty.worldgen;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomPatchFeature.class)
public abstract class LavenderRandomPatchMixin {
    @Unique
    private static final ResourceLocation FRMC$LAVENDER_SPAWN = new ResourceLocation("manors_bounty", "lavender_spawn");

    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At("HEAD"), cancellable = true)
    private void frmc$placeLavender(FeaturePlaceContext<RandomPatchConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        RandomPatchConfiguration config = context.config();
        PlacedFeature placedFeature = config.feature().value();
        List<PlacementModifier> modifiers = placedFeature.placement();
        if (modifiers.size() != 1 || !(modifiers.get(0) instanceof BlockPredicateFilter filter)) {
            return;
        }
        ConfiguredFeature<?, ?> candidate = placedFeature.feature().value();
        if (candidate.feature() != Feature.SIMPLE_BLOCK) {
            return;
        }

        WorldGenLevel level = context.level();
        ConfiguredFeature<?, ?> lavender = level.registryAccess().registry(Registries.CONFIGURED_FEATURE)
                .map(registry -> registry.get(FRMC$LAVENDER_SPAWN))
                .orElse(null);
        if (lavender == null || lavender.config() != config) {
            return;
        }

        ChunkGenerator generator = context.chunkGenerator();
        PlacementContext placement = new PlacementContext(level, generator, Optional.empty());
        BlockPredicateFilterInvoker predicate = (BlockPredicateFilterInvoker) filter;
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int xzSpread = config.xzSpread() + 1;
        int ySpread = config.ySpread() + 1;
        boolean placed = false;

        // Keep the vanilla roll order, filter and configured feature while avoiding per-candidate streams.
        for (int i = 0; i < config.tries(); i++) {
            pos.setWithOffset(origin, random.nextInt(xzSpread) - random.nextInt(xzSpread),
                    random.nextInt(ySpread) - random.nextInt(ySpread),
                    random.nextInt(xzSpread) - random.nextInt(xzSpread));
            if (predicate.frmc$shouldPlace(placement, random, pos) && candidate.place(level, generator, random, pos)) {
                placed = true;
            }
        }
        cir.setReturnValue(placed);
    }
}
