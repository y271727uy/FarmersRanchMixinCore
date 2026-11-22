package com.y271727uy.FRMC.mixin.untamed_wilds;

import com.y271727uy.FRMC.integration.computatio.CitadelGroundPath;
import com.y271727uy.FRMC.integration.computatio.DirectNavigationPath;
import com.y271727uy.FRMC.integration.untamedwilds.UntamedWildsNavigation;
import com.y271727uy.FRMC.mixin.minecraft.pathfinding.PathNavigationAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import untamedwilds.entity.ComplexMobTerrestrial;

import java.util.Set;

/**
 * Short, flat, obstacle-free walks of Untamed Wilds land animals skip the amphibious A* search and use a
 * single-node direct path, the same route Alex's Caves walkers already get. Anything in water, on a slope,
 * through a danger block or further than 10 blocks still runs the original search.
 */
@Mixin(PathNavigation.class)
public abstract class TerrestrialPathNavigationMixin {
    private static final int FRMC$MAX_DIRECT_DISTANCE = 10;
    private static final float FRMC$MAX_DIRECT_WIDTH = 2.0F;

    @Shadow @Final protected Mob mob;
    @Shadow protected Path path;
    @Shadow protected boolean hasDelayedRecomputation;

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"),
            cancellable = true, require = 1)
    private void frmc$tryUntamedWildsShortRoute(Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
                                                int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        if (!(mob instanceof ComplexMobTerrestrial)
                || !UntamedWildsNavigation.isTerrestrialNavigation((PathNavigation) (Object) this)
                || targets.size() != 1 || accuracy > 1 || mob.getBbWidth() >= FRMC$MAX_DIRECT_WIDTH
                || !(mob.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            return;
        }
        BlockPos target = targets.iterator().next();
        BlockPos start = mob.blockPosition();
        if (start.equals(target) || start.distManhattan(target) > FRMC$MAX_DIRECT_DISTANCE
                || start.distSqr(target) >= (double) followRange * followRange
                || !CitadelGroundPath.canWalkDirectly(level, mob, target)) {
            return;
        }
        var state = (PathNavigationAccessor) this;
        state.frmc$setTargetPos(target);
        state.frmc$setReachRange(accuracy);
        state.frmc$resetStuckTimeout();
        cir.setReturnValue(new DirectNavigationPath(mob, target, BlockPathTypes.WALKABLE));
    }

    @Inject(method = "shouldRecomputePath", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$recomputeBlockedUntamedWildsRoute(BlockPos changedBlock, CallbackInfoReturnable<Boolean> cir) {
        if (!hasDelayedRecomputation && path instanceof DirectNavigationPath direct
                && !direct.isDone() && direct.crosses(changedBlock)) {
            cir.setReturnValue(true);
        }
    }
}
