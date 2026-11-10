package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.VesperFlyAndHangGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.VesperEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VesperFlyAndHangGoal.class)
public abstract class VesperHangRetryMixin {
    @Shadow(remap = false) private VesperEntity entity;
    @Unique private long frmc$nextSearchTick = Long.MIN_VALUE;

    @WrapOperation(method = "canUse", at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/ai/VesperFlyAndHangGoal;getPosition()Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 frmc$backOffFailedSearch(VesperFlyAndHangGoal goal, Operation<Vec3> original) {
        long now = entity.level().getGameTime();
        if (now < frmc$nextSearchTick) {
            return null;
        }
        Vec3 target = original.call(goal);
        if (target == null) {
            frmc$nextSearchTick = now + 8L;
        }
        return target;
    }

    @WrapOperation(method = "start", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$backOffFailedStart(PathNavigation navigation, double x, double y, double z, double speed,
            Operation<Boolean> original) {
        boolean started = original.call(navigation, x, y, z, speed);
        if (!started) {
            frmc$nextSearchTick = entity.level().getGameTime() + 8L;
        }
        return started;
    }
}
