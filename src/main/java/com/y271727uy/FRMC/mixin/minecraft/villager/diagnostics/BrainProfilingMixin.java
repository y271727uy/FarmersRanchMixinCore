package com.y271727uy.FRMC.mixin.minecraft.villager.diagnostics;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfileSession.Phase;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfiler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Times the four Brain.tick phases for vanilla villagers while a profiling session runs. Read-only. */
// Radium's priority-1000 overwrite still calls BehaviorControl.tryStart; wrap that call in its replacement.
@Mixin(value = Brain.class, priority = 1100)
public abstract class BrainProfilingMixin {
    @WrapMethod(method = "tick", require = 0)
    private void frmc$profileVillagerBrain(ServerLevel level, LivingEntity entity, Operation<Void> original) {
        if (!VillagerProfiler.tracks(entity)) {
            original.call(level, entity);
            return;
        }
        LivingEntity previous = VillagerProfiler.enterBrain(entity);
        long started = System.nanoTime();
        try {
            original.call(level, entity);
        } finally {
            VillagerProfiler.recordBrainTick(entity, System.nanoTime() - started);
            VillagerProfiler.exitBrain(previous);
        }
    }

    @WrapOperation(method = "startEachNonRunningBehavior", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;tryStart(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z"))
    private boolean frmc$profileBehaviorStart(BehaviorControl<?> behavior, ServerLevel level, LivingEntity entity,
                                              long gameTime, Operation<Boolean> original) {
        if (!VillagerProfiler.tracks(entity)) {
            return original.call(behavior, level, entity, gameTime);
        }
        Object previous = VillagerProfiler.enterBehavior(behavior);
        long started = System.nanoTime();
        try {
            return original.call(behavior, level, entity, gameTime);
        } finally {
            VillagerProfiler.recordBehaviorStart(behavior, System.nanoTime() - started);
            VillagerProfiler.exitBehavior(previous);
        }
    }

    @WrapOperation(method = "tickEachRunningBehavior", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;tickOrStop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V"))
    private void frmc$profileBehaviorTick(BehaviorControl<?> behavior, ServerLevel level, LivingEntity entity,
                                          long gameTime, Operation<Void> original) {
        if (!VillagerProfiler.tracks(entity)) {
            original.call(behavior, level, entity, gameTime);
            return;
        }
        Object previous = VillagerProfiler.enterBehavior(behavior);
        long started = System.nanoTime();
        try {
            original.call(behavior, level, entity, gameTime);
        } finally {
            VillagerProfiler.recordBehaviorTick(behavior, System.nanoTime() - started);
            VillagerProfiler.exitBehavior(previous);
        }
    }

    /** Attributes WALK_TARGET writes made during a tracked villager's brain tick. Never changes the write. */
    @Inject(method = "setMemoryInternal", at = @At("HEAD"), require = 0)
    private void frmc$profileMemoryWrite(MemoryModuleType<?> type, Optional<? extends ExpirableValue<?>> value,
                                         CallbackInfo ci) {
        if (VillagerProfiler.isActive()) {
            VillagerProfiler.recordMemoryWrite(type, value);
        }
    }

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;forgetOutdatedMemories()V"))
    private void frmc$profileForget(Brain<?> brain, Operation<Void> original, ServerLevel level, LivingEntity entity) {
        if (!VillagerProfiler.tracks(entity)) {
            original.call(brain);
            return;
        }
        long started = System.nanoTime();
        try {
            original.call(brain);
        } finally {
            VillagerProfiler.recordPhase(Phase.FORGET_MEMORIES, System.nanoTime() - started);
        }
    }

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;tickSensors(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void frmc$profileSensors(Brain<?> brain, ServerLevel level, LivingEntity entity, Operation<Void> original) {
        frmc$profilePhase(Phase.SENSORS, brain, level, entity, original);
    }

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;startEachNonRunningBehavior(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void frmc$profileStart(Brain<?> brain, ServerLevel level, LivingEntity entity, Operation<Void> original) {
        frmc$profilePhase(Phase.START_BEHAVIORS, brain, level, entity, original);
    }

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;tickEachRunningBehavior(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void frmc$profileRunning(Brain<?> brain, ServerLevel level, LivingEntity entity, Operation<Void> original) {
        frmc$profilePhase(Phase.TICK_BEHAVIORS, brain, level, entity, original);
    }

    @Unique
    private static void frmc$profilePhase(Phase phase, Brain<?> brain, ServerLevel level, LivingEntity entity,
                                     Operation<Void> original) {
        if (!VillagerProfiler.tracks(entity)) {
            original.call(brain, level, entity);
            return;
        }
        long started = System.nanoTime();
        try {
            original.call(brain, level, entity);
        } finally {
            VillagerProfiler.recordPhase(phase, System.nanoTime() - started);
        }
    }
}
