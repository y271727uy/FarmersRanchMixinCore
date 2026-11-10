package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.CorrodentAttackGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CorrodentAttackGoal.class)
public abstract class CorrodentEvadeRetryMixin {
    @Shadow(remap = false) private CorrodentEntity entity;
    @Unique private long frmc$nextEvadeSearchTick = Long.MIN_VALUE;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = false, target =
            "Lcom/github/alexmodguy/alexscaves/server/entity/ai/CorrodentAttackGoal;generateEvadePosition(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 frmc$backOffFailedEvadeSearch(CorrodentAttackGoal goal, BlockPos around, Operation<Vec3> original) {
        long now = entity.level().getGameTime();
        if (now < frmc$nextEvadeSearchTick) {
            return null;
        }
        Vec3 target = original.call(goal, around);
        if (target == null) {
            frmc$nextEvadeSearchTick = now + 8L;
        }
        return target;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$backOffFailedEvadePath(PathNavigation navigation, double x, double y, double z, double speed,
            Operation<Boolean> original) {
        boolean started = original.call(navigation, x, y, z, speed);
        if (!started) {
            frmc$nextEvadeSearchTick = entity.level().getGameTime() + 8L;
        }
        return started;
    }
}
