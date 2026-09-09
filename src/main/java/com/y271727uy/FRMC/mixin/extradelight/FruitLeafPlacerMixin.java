package com.y271727uy.FRMC.mixin.extradelight;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FancyFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageAttachment;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer.FoliageSetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.vomiter.extradelight.worldgen.placers.FruitLeafPlacer", remap = false)
public abstract class FruitLeafPlacerMixin extends FancyFoliagePlacer {
    protected FruitLeafPlacerMixin(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset, height);
    }

    @Dynamic
    @Inject(method = {"createFoliage", "m_213633_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$createFoliageWithoutPerBlockLogging(
            LevelSimulatedReader level,
            FoliageSetter foliageSetter,
            RandomSource random,
            TreeConfiguration config,
            int maxFreeTreeHeight,
            FoliageAttachment attachment,
            int foliageHeight,
            int foliageRadius,
            int offset,
            CallbackInfo ci) {
        for (int localY = offset; localY >= offset - foliageHeight; localY--) {
            int range = foliageRadius + (localY != offset && localY != offset - foliageHeight ? 1 : 0);
            this.placeLeavesRow(level, foliageSetter, random, config, attachment.pos(), range, localY, attachment.doubleTrunk());
        }
        ci.cancel();
    }
}
