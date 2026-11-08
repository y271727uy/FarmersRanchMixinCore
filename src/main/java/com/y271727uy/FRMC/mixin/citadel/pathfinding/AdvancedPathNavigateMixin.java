package com.y271727uy.FRMC.mixin.citadel.pathfinding;

import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.AbstractAdvancedPathNavigate;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.AdvancedPathNavigate;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.IPassabilityNavigator;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.ITallWalker;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.PathResult;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.Pathfinding;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.pathjobs.AbstractPathJob;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.pathjobs.ICustomSizeNavigator;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.pathjobs.PathJobMoveToLocation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.computatio.CitadelGroundPathJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvancedPathNavigate.class, remap = false)
public abstract class AdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate {
    @Shadow private PathResult<AbstractPathJob> pathResult;
    @Shadow public boolean overrideDefaultDimensions;

    @Unique private boolean frmc$useCitadelFallback;

    protected AdvancedPathNavigateMixin(Mob mob, Level level) {
        super(mob, level);
    }

    @WrapOperation(method = "moveToXYZ", at = @At(value = "NEW", target =
            "com/github/alexthe666/citadel/server/entity/pathfinding/raycoms/pathjobs/PathJobMoveToLocation"))
    private PathJobMoveToLocation frmc$captureGroundJob(Level level, BlockPos start, BlockPos target,
                                                        int range, LivingEntity entity,
                                                        Operation<PathJobMoveToLocation> original,
                                                        double x, double y, double z, double speed) {
        PathJobMoveToLocation job = original.call(level, start, target, range, entity);
        if (frmc$useCitadelFallback || overrideDefaultDimensions || getPathingOptions().isFlying()
                || getPathingOptions().canUseRails() || ourEntity instanceof IPassabilityNavigator
                || ourEntity instanceof ITallWalker || ourEntity instanceof ICustomSizeNavigator
                || !(speed >= 0.1D && speed <= 2.0D)
                || !(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isSameThread()
                || Pathfinding.isDebug() || AbstractPathJob.trackingMap.containsValue(ourEntity.getUUID())
                || Pathfinding.getExecutor().getQueue().size() >= 8) {
            return job;
        }
        ((CitadelGroundPathJob) job).frmc$capture(ourEntity, start, target, range);
        return job;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexthe666/citadel/server/entity/pathfinding/raycoms/AdvancedPathNavigate;"
                    + "processCompletedCalculationResult()Z"), remap = true, cancellable = true)
    private void frmc$validateGroundJob(CallbackInfo ci) {
        if (!(pathResult.getJob() instanceof CitadelGroundPathJob job)) {
            return;
        }
        var candidate = job.frmc$takeCandidate();
        if (candidate == null) {
            return;
        }
        if (!ourEntity.isAlive() || ourEntity.getNavigation() != (Object) this) {
            stop();
        } else if (!candidate.canApply(ourEntity, pathResult.getPath())) {
            double speed = walkSpeedFactor;
            BlockPos target = candidate.target();
            stop();
            frmc$useCitadelFallback = true;
            try {
                moveToXYZ(target.getX(), target.getY(), target.getZ(), speed);
            } finally {
                frmc$useCitadelFallback = false;
            }
        } else {
            return;
        }
        // A replacement job may still be computing when this tick ends.
        ci.cancel();
    }
}
