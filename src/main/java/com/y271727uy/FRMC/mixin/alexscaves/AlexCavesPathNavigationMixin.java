package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ai.GroundPathNavigatorNoSpin;
import com.github.alexmodguy.alexscaves.server.entity.ai.FlightPathNavigatorNoSpin;
import com.github.alexmodguy.alexscaves.server.entity.ai.SemiAquaticPathNavigator;
import com.github.alexmodguy.alexscaves.server.entity.living.BoundroidWinchEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.BrainiacEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GammaroachEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GloomothEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.HullbreakerEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.LanternfishEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.MagnetronEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.NotorEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.NucleeperEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.RadgillEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.RaycatEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.TeletorEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.TrilocarisEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.UnderzealotEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.VallumraptorEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.WatcherEntity;
import com.y271727uy.FRMC.integration.computatio.CitadelGroundPath;
import com.y271727uy.FRMC.integration.computatio.DirectNavigationPath;
import com.y271727uy.FRMC.integration.computatio.FlightFastPath;
import com.y271727uy.FRMC.integration.computatio.WaterFastPath;
import com.y271727uy.FRMC.mixin.minecraft.pathfinding.PathNavigationAccessor;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class AlexCavesPathNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Shadow protected Path path;
    @Shadow protected boolean hasDelayedRecomputation;
    @Unique private long frmc$nextHullbreakerPathRefresh;
    @Unique private LivingEntity frmc$hullbreakerAttackTarget;

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"),
            cancellable = true, require = 1)
    private void frmc$limitHullbreakerPathRefresh(Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
            int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        Class<?> navigator = getClass();
        if (!(mob instanceof HullbreakerEntity) || navigator != WaterBoundPathNavigation.class
                || targets.size() != 1 || accuracy != 1 || !(mob.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) {
            return;
        }
        long now = level.getGameTime();
        LivingEntity attackTarget = mob.getTarget();
        if (!hasDelayedRecomputation && path != null && !path.isDone() && path.canReach()
                && attackTarget == frmc$hullbreakerAttackTarget && now < frmc$nextHullbreakerPathRefresh
                && path.getTarget().distSqr(targets.iterator().next()) <= 4.0D) {
            cir.setReturnValue(path);
            return;
        }
        frmc$hullbreakerAttackTarget = attackTarget;
        frmc$nextHullbreakerPathRefresh = now + 4;
    }

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"),
            cancellable = true, require = 1)
    private void frmc$tryShortRoute(Set<BlockPos> targets, int regionOffset, boolean offsetUpward,
                                    int accuracy, float followRange, CallbackInfoReturnable<Path> cir) {
        Class<?> navigator = getClass();
        boolean walking = (navigator == GroundPathNavigatorNoSpin.class
                        && (mob instanceof VallumraptorEntity || mob instanceof NucleeperEntity
                                || mob instanceof BrainiacEntity || mob instanceof UnderzealotEntity
                                || mob instanceof WatcherEntity))
                || (navigator == GroundPathNavigation.class
                        && ((mob instanceof MagnetronEntity magnetron && !magnetron.isFormed())
                                || (mob instanceof BoundroidWinchEntity winch && !winch.isLatched())
                                || mob instanceof GammaroachEntity || mob instanceof RaycatEntity
                                || mob instanceof GingerbreadManEntity || mob instanceof GloomothEntity
                                || mob instanceof VesperEntity
                                || (mob instanceof CorrodentEntity corrodent && !corrodent.isDigging())));
        boolean radgill = mob instanceof RadgillEntity && navigator.getSuperclass() == WaterBoundPathNavigation.class
                && mob.getNavigation() == (Object) this;
        boolean swimming = radgill || (navigator == SemiAquaticPathNavigator.class && mob instanceof TrilocarisEntity)
                || (navigator == WaterBoundPathNavigation.class && mob instanceof LanternfishEntity fish && fish.isCircling());
        boolean vesperFlying = navigator == FlightPathNavigatorNoSpin.class
                && mob instanceof VesperEntity vesper && !vesper.isHanging();
        boolean flying = vesperFlying || ((Object) this instanceof FlyingPathNavigation
                && (mob instanceof NotorEntity || mob instanceof TeletorEntity
                        || mob instanceof GloomothEntity || mob instanceof WatcherEntity));
        float maxWidth = mob instanceof GammaroachEntity || mob instanceof BrainiacEntity
                || mob instanceof VesperEntity ? 1.5F : 1.0F;
        if ((!walking && !swimming && !flying) || targets.size() != 1 || accuracy > 1
                || mob.getBbWidth() >= maxWidth || !(mob.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread()) {
            return;
        }

        BlockPos target = targets.iterator().next();
        BlockPos start = mob.blockPosition();
        if (start.equals(target) || start.distManhattan(target) > 10
                || start.distSqr(target) >= (double) followRange * followRange) {
            return;
        }
        BlockPathTypes type;
        if (walking) {
            if (!CitadelGroundPath.canWalkDirectly(level, mob, target)) {
                return;
            }
            type = BlockPathTypes.WALKABLE;
        } else if (swimming) {
            if (mob.getBbHeight() > 1.0F) {
                return;
            }
            var destination = Vec3.atBottomCenterOf(target);
            var acid = radgill ? ACFluidRegistry.ACID_FLUID_TYPE.get() : null;
            boolean clear = acid != null && mob.getFluidTypeHeight(acid) > 0
                    ? WaterFastPath.clear(level, mob, mob.position(), destination,
                            state -> state.getFluidState().isSource() && state.getFluidState().getFluidType() == acid)
                    : mob.isInWater() && WaterFastPath.clear(level, mob, mob.position(), destination);
            if (!clear) {
                return;
            }
            type = BlockPathTypes.WATER;
        } else {
            // Vesper's vanilla path search can target midair. Its stability check tests whether the goal block can be stood on.
            if (!FlightFastPath.canFlyDirectly(level, mob, target)
                    || (!vesperFlying && !((PathNavigation) (Object) this).isStableDestination(target))) {
                return;
            }
            type = BlockPathTypes.OPEN;
        }

        var state = (PathNavigationAccessor) this;
        state.frmc$setTargetPos(target);
        state.frmc$setReachRange(accuracy);
        state.frmc$resetStuckTimeout();
        cir.setReturnValue(new DirectNavigationPath(mob, target, type));
    }

    @Inject(method = "shouldRecomputePath", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$recomputeBlockedShortRoute(BlockPos changedBlock, CallbackInfoReturnable<Boolean> cir) {
        if (!hasDelayedRecomputation && path instanceof DirectNavigationPath direct
                && !direct.isDone() && direct.crosses(changedBlock)) {
            cir.setReturnValue(true);
        }
    }
}
