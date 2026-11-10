package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.ForsakenAttackGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.WatcherAttackGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WatcherAttackGoal.class, ForsakenAttackGoal.class})
public abstract class ForlornPursuitRetryMixin {
    @Unique private long frmc$nextPursuitTick;
    @Unique private Entity frmc$failedTarget;
    @Unique private BlockPos frmc$failedPosition;
    @Unique private PathNavigation frmc$failedNavigator;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/entity/Entity;D)Z"),
            require = 1)
    private boolean frmc$backOffFailedPursuit(PathNavigation navigation, Entity target, double speed,
            Operation<Boolean> original) {
        long now = target.level().getGameTime();
        BlockPos position = target.blockPosition();
        if (target == frmc$failedTarget && navigation == frmc$failedNavigator
                && position.equals(frmc$failedPosition) && now < frmc$nextPursuitTick) {
            return false;
        }
        boolean started = original.call(navigation, target, speed);
        if (started) {
            frmc$clearFailedPursuit();
        } else {
            frmc$failedTarget = target;
            frmc$failedPosition = position;
            frmc$failedNavigator = navigation;
            frmc$nextPursuitTick = now + 4L;
        }
        return started;
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 1)
    private void frmc$resetPursuitRetry(CallbackInfo ci) {
        frmc$clearFailedPursuit();
    }

    @Unique
    private void frmc$clearFailedPursuit() {
        frmc$failedTarget = null;
        frmc$failedPosition = null;
        frmc$failedNavigator = null;
    }
}
