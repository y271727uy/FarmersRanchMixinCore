package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.GumWormLeapRandomlyGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GumWormLeapRandomlyGoal.class)
public abstract class GumWormLeapNavigationMixin {
    @Shadow(remap = false) @Final private GumWormEntity entity;
    @Unique private long frmc$nextLeapSearchTick = Long.MIN_VALUE;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$delayFailedLeapSearch(CallbackInfoReturnable<Boolean> cir) {
        if (entity.level() instanceof ServerLevel level && level.getGameTime() < frmc$nextLeapSearchTick) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "canUse", at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/ai/GumWormLeapRandomlyGoal;findLeapFromPosition()Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 frmc$backOffEmptyLeapSearch(GumWormLeapRandomlyGoal goal, Operation<Vec3> original) {
        Vec3 target = original.call(goal);
        if (target == null) {
            frmc$delayLeapSearch();
        }
        return target;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$backOffFailedLeapPath(PathNavigation navigation, double x, double y, double z, double speed,
            Operation<Boolean> original) {
        boolean started = original.call(navigation, x, y, z, speed);
        if (!started) {
            // The active route already uses vanilla's fixed-target reuse. Back off only when it cannot start.
            frmc$delayLeapSearch();
        }
        return started;
    }

    @Unique
    private void frmc$delayLeapSearch() {
        if (entity.level() instanceof ServerLevel level) {
            frmc$nextLeapSearchTick = level.getGameTime() + 8L;
        }
    }
}
