package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.AerolopeEntity;
import com.cloudmeow.delightoflight.entity.ai.aerolope.GoatSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = GoatSensor.class, remap = false)
public abstract class GoatSensorMixin {
    @Unique private static final int FRMC$REFRESH_WINDOW = 20;

    @Inject(method = {"doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            "m_5578_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"},
            at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$skipScanDuringRamCooldown(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        // GoatSensor uses the default 20-tick scan rate. Keep the final scan before RAM can start.
        if (entity instanceof AerolopeEntity
                && entity.getBrain().getMemory(MemoryModuleType.RAM_COOLDOWN_TICKS).orElse(0) > FRMC$REFRESH_WINDOW) {
            ci.cancel();
        }
    }
}
