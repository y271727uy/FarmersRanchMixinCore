package com.y271727uy.FRMC.mixin.minecraft.villager.diagnostics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfiler;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Times the real vanilla A* search inside createPath. Early returns (reused path, empty targets) and other
 * mixins' fast paths never reach PathFinder.findPath, so they are not counted. Read-only.
 */
@Mixin(PathNavigation.class)
public abstract class PathSearchProfilingMixin {
    @WrapOperation(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/pathfinder/PathFinder;findPath(Lnet/minecraft/world/level/PathNavigationRegion;Lnet/minecraft/world/entity/Mob;Ljava/util/Set;FIF)Lnet/minecraft/world/level/pathfinder/Path;"))
    private Path frmc$profileVillagerPathSearch(PathFinder finder, PathNavigationRegion region, Mob mob,
                                                Set<BlockPos> targets, float maxRange, int accuracy,
                                                float depthMultiplier, Operation<Path> original) {
        if (!VillagerProfiler.tracksPath(mob)) {
            return original.call(finder, region, mob, targets, maxRange, accuracy, depthMultiplier);
        }
        long started = System.nanoTime();
        Path path = original.call(finder, region, mob, targets, maxRange, accuracy, depthMultiplier);
        VillagerProfiler.recordPath(mob, targets, path, System.nanoTime() - started);
        return path;
    }
}
