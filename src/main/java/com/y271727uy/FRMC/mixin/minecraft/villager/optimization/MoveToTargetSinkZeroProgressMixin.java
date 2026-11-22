package com.y271727uy.FRMC.mixin.minecraft.villager.optimization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfiler;
import com.y271727uy.FRMC.capability.villager.optimization.ZeroProgressPath;
import com.y271727uy.FRMC.config.VillagerOptimizationConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla villagers only. When the start check computes a partial path that ends in the villager's own column,
 * report the computation as failed instead of starting a walk that finishes instantly. Vanilla's failure branch
 * then erases WALK_TARGET exactly as stop() would have, CANT_REACH_WALK_TARGET_SINCE was already set by
 * tryComputePath, and the 0-39 tick cooldown is the same one vanilla applies to stuck villagers in stop().
 * The tick() re-path call site is left alone.
 */
@Mixin(MoveToTargetSink.class)
public abstract class MoveToTargetSinkZeroProgressMixin {
    @Shadow private int remainingCooldown;
    @Shadow private Path path;

    @WrapOperation(method = "checkExtraStartConditions", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/MoveToTargetSink;tryComputePath(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ai/memory/WalkTarget;J)Z"))
    private boolean frmc$rejectZeroProgressPath(MoveToTargetSink self, Mob mob, WalkTarget target, long gameTime,
                                                Operation<Boolean> original, ServerLevel level, Mob owner) {
        boolean computed = original.call(self, mob, target, gameTime);
        if (!computed || mob.getType() != EntityType.VILLAGER || !VillagerOptimizationConfig.zeroProgressPathCooldown()) {
            return computed;
        }
        Path computedPath = this.path;
        Node end = computedPath == null ? null : computedPath.getEndNode();
        if (end == null) {
            return true;
        }
        BlockPos position = mob.blockPosition();
        if (!ZeroProgressPath.isZeroProgress(computedPath.canReach(), end.x, end.y, end.z,
                position.getX(), position.getY(), position.getZ())) {
            return true;
        }
        this.path = null;
        this.remainingCooldown = level.getRandom().nextInt(ZeroProgressPath.RETRY_COOLDOWN_BOUND);
        VillagerProfiler.recordOptimization(mob, "zero-progress-path");
        return false;
    }
}
