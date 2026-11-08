package com.y271727uy.FRMC.mixin.delighto_flight;

import com.y271727uy.FRMC.integration.delighto_flight.AllayChefPathfinding;
import com.y271727uy.FRMC.integration.delighto_flight.ChefFlightPath;
import com.y271727uy.FRMC.integration.delighto_flight.ChefWalkTarget;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
    @Unique private static final int FRMC$FAST_PATH_RETRY_TICKS = 20;

    @Shadow @Final protected Mob mob;
    @Shadow protected Path path;
    @Shadow private BlockPos targetPos;
    @Shadow private int reachRange;
    @Shadow protected boolean hasDelayedRecomputation;

    @Unique private BlockPos frmc$failedStart;
    @Unique private BlockPos frmc$failedTarget;
    @Unique private long frmc$retryAfter;

    @Shadow
    private void resetStuckTimeout() {
        throw new AssertionError();
    }

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;"),
            cancellable = true)
    private void frmc$findChefPath(Set<BlockPos> targets, int regionOffset, boolean above, int accuracy,
                                  float followRange, CallbackInfoReturnable<Path> cir) {
        // Vanilla has already checked whether navigation can run and whether its active path can be reused.
        if (accuracy != 0 || above || targets.size() != 1 || !(mob instanceof Allay allay)
                || !((Object) this instanceof FlyingPathNavigation)) {
            return;
        }
        WalkTarget work = allay.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
        BlockPos target = targets.iterator().next();
        if (work instanceof ChefWalkTarget && target.equals(work.getTarget().currentBlockPosition())) {
            BlockPos start = allay.blockPosition();
            long gameTime = allay.level().getGameTime();
            if (gameTime < frmc$retryAfter && start.equals(frmc$failedStart) && target.equals(frmc$failedTarget)) {
                return;
            }
            Path path = AllayChefPathfinding.find(allay, target, work.getCloseEnoughDist(), followRange);
            if (path == null) {
                frmc$rememberFailure(start, target, gameTime);
                return;
            }
            this.targetPos = target;
            this.reachRange = accuracy;
            resetStuckTimeout();
            cir.setReturnValue(path);
        }
    }

    @Redirect(method = "followThePath", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/Path;advance()V"))
    private void frmc$finishOnlyInsideWorkRange(Path advancing) {
        if (advancing instanceof ChefFlightPath flight && advancing.getNextNodeIndex() == advancing.getNodeCount() - 1
                && !flight.hasArrived(mob.blockPosition())) {
            // Vanilla's waypoint tolerance can advance while the allay is still a block too low.
            return;
        }
        advancing.advance();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void frmc$backOffAfterUnfinishedFlight(CallbackInfo ci) {
        if (path instanceof ChefFlightPath flight) {
            BlockPos start = mob.blockPosition();
            if (!flight.hasArrived(start)) {
                frmc$rememberFailure(start, flight.getTarget(), mob.level().getGameTime());
            }
        }
    }

    @Unique
    private void frmc$rememberFailure(BlockPos start, BlockPos target, long gameTime) {
        frmc$failedStart = start.immutable();
        frmc$failedTarget = target.immutable();
        frmc$retryAfter = gameTime + FRMC$FAST_PATH_RETRY_TICKS;
    }

    @Inject(method = "shouldRecomputePath", at = @At("HEAD"), cancellable = true)
    private void frmc$watchFlightCorridor(BlockPos changedBlock, CallbackInfoReturnable<Boolean> cir) {
        if (path instanceof ChefFlightPath flight) {
            // A straight flight can have one node, so vanilla's node-count radius is too small.
            cir.setReturnValue(!hasDelayedRecomputation && !flight.isDone() && flight.crosses(changedBlock));
        }
    }

    @Inject(method = "canCutCorner", at = @At("HEAD"), cancellable = true)
    private void frmc$keepCheckedFlightSegments(BlockPathTypes type, CallbackInfoReturnable<Boolean> cir) {
        if (path instanceof ChefFlightPath) {
            cir.setReturnValue(false);
        }
    }
}
