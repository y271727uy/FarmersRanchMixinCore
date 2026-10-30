package com.y271727uy.FRMC.mixin.forestry;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.y271727uy.FRMC.integration.sereneseasons.SeasonalCropBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Restricts only fruit ripening; leaf handlers, caterpillars and tree effects still tick. */
@Pseudo
@Mixin(targets = "forestry.arboriculture.tiles.TileLeaves", remap = false)
public abstract class TileLeavesSeasonalGrowthMixin {
    @ModifyExpressionValue(
        method = "onBlockTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V",
        at = @At(
            value = "INVOKE",
            target = "Lforestry/api/genetics/IGenome;getActiveValue(Lforestry/api/genetics/alleles/IFloatChromosome;)F",
            remap = false
        ),
        require = 1,
        remap = false
    )
    private float frmc$seasonalFruitSappiness(
        float sappiness,
        Level level,
        BlockPos pos,
        BlockState state,
        RandomSource random
    ) {
        return !level.isClientSide && !SeasonalCropBridge.isGrowthAllowed(level, pos, state)
            ? 0.0F : sappiness;
    }
}
