package com.y271727uy.FRMC.mixin.minecraft.villager.diagnostics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfiler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Times only the sensor scans that actually run (doTick), not the per-tick countdown. Read-only. */
@Mixin(Sensor.class)
public abstract class SensorProfilingMixin {
    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/Sensor;doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void frmc$profileVillagerSensor(Sensor<?> sensor, ServerLevel level, LivingEntity entity,
                                            Operation<Void> original) {
        if (!VillagerProfiler.tracks(entity)) {
            original.call(sensor, level, entity);
            return;
        }
        long started = System.nanoTime();
        try {
            original.call(sensor, level, entity);
        } finally {
            VillagerProfiler.recordSensor(sensor, System.nanoTime() - started);
        }
    }
}
