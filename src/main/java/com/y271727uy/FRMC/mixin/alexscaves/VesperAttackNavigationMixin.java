package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.FlightPathNavigatorNoSpin;
import com.github.alexmodguy.alexscaves.server.entity.ai.VesperAttackGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.mixin.minecraft.pathfinding.PathNavigationAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VesperAttackGoal.class)
public abstract class VesperAttackNavigationMixin {
    @Shadow(remap = false) private VesperEntity entity;
    @Unique private Path frmc$flightPath;
    @Unique private Entity frmc$flightTarget;
    @Unique private boolean frmc$orbitPath;
    @Unique private long frmc$nextFlightRefresh;

    @Inject(method = "start", at = @At("HEAD"), require = 1)
    private void frmc$resetFlightPath(CallbackInfo ci) {
        frmc$flightPath = null;
        frmc$flightTarget = null;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"),
            require = 1)
    private boolean frmc$reusePursuitPath(PathNavigation navigation, Entity target, double speed,
            Operation<Boolean> original) {
        if (frmc$reuseFlightPath(navigation, target, target.blockPosition(), false, speed)) {
            return true;
        }
        boolean started = original.call(navigation, target, speed);
        frmc$rememberFlightPath(navigation, target, false, started);
        return started;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$reuseOrbitPath(PathNavigation navigation, double x, double y, double z, double speed,
            Operation<Boolean> original) {
        Entity target = entity.getTarget();
        if (frmc$reuseFlightPath(navigation, target, BlockPos.containing(x, y, z), true, speed)) {
            return true;
        }
        boolean started = original.call(navigation, x, y, z, speed);
        frmc$rememberFlightPath(navigation, target, true, started);
        return started;
    }

    @Unique
    private boolean frmc$reuseFlightPath(PathNavigation navigation, Entity target, BlockPos destination,
            boolean orbit, double speed) {
        if (!(navigation instanceof FlightPathNavigatorNoSpin) || !entity.isFlying()
                || frmc$flightPath == null || navigation.getPath() != frmc$flightPath
                || target != frmc$flightTarget || orbit != frmc$orbitPath
                || entity.level().getGameTime() >= frmc$nextFlightRefresh
                || frmc$flightPath.isDone() || !frmc$flightPath.canReach() || navigation.isStuck()
                || ((PathNavigationAccessor) navigation).frmc$hasDelayedRecomputation()
                || frmc$flightPath.getTarget().distSqr(destination) > 4.0D) {
            return false;
        }
        navigation.setSpeedModifier(speed);
        return true;
    }

    @Unique
    private void frmc$rememberFlightPath(PathNavigation navigation, Entity target, boolean orbit, boolean started) {
        if (started && entity.isFlying() && navigation instanceof FlightPathNavigatorNoSpin) {
            frmc$flightPath = navigation.getPath();
            frmc$flightTarget = target;
            frmc$orbitPath = orbit;
            frmc$nextFlightRefresh = entity.level().getGameTime() + 4L;
        } else {
            frmc$flightPath = null;
            frmc$flightTarget = null;
        }
    }
}
