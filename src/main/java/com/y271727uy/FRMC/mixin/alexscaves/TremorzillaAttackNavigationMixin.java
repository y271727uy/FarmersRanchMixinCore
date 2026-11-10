package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.TremorzillaAttackGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import com.github.alexthe666.citadel.server.entity.pathfinding.raycoms.AdvancedPathNavigate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TremorzillaAttackGoal.class)
public abstract class TremorzillaAttackNavigationMixin {
    @Shadow(remap = false) private TremorzillaEntity tremorzilla;
    @Shadow(remap = false) private Vec3 lastNavToPos;
    @Unique private PathNavigation frmc$lastNavigator;
    @Unique private LivingEntity frmc$lastTarget;

    @Inject(method = "tick", at = @At("HEAD"), require = 1)
    private void frmc$resetTrackingWhenChaseChanges(CallbackInfo ci) {
        var navigator = tremorzilla.getNavigation();
        var target = tremorzilla.getTarget();
        if (navigator != frmc$lastNavigator || target != frmc$lastTarget) {
            lastNavToPos = null;
            frmc$lastNavigator = navigator;
            frmc$lastTarget = target;
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"),
            require = 1)
    private boolean frmc$recordAcceptedDestination(PathNavigation navigation, Entity target, double speed,
            Operation<Boolean> original) {
        boolean accepted = original.call(navigation, target, speed);
        BlockPos destination = navigation instanceof AdvancedPathNavigate citadel
                ? citadel.getDestination() : navigation.getTargetPos();
        // Citadel can report success while keeping an older job that is still running.
        if (accepted && target.blockPosition().equals(destination)) {
            lastNavToPos = target.position();
        }
        return accepted;
    }

    @Inject(method = "stop", at = @At("TAIL"), require = 1)
    private void frmc$clearChaseTracking(CallbackInfo ci) {
        frmc$lastNavigator = null;
        frmc$lastTarget = null;
    }
}
