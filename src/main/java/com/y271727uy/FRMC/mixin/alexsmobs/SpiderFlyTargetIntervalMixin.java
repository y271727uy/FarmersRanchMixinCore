package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityFly;
import com.github.alexthe666.alexsmobs.event.ServerEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Alex's Mobs gives every spider a fly target goal with random interval 1, so each target evaluation
 * scans the spider's follow range for flies. Vanilla spiders look for players with interval 10;
 * use the same cadence for flies. Only the {@link EntityFly} goal is touched.
 */
@Mixin(value = ServerEvents.class, remap = false)
public abstract class SpiderFlyTargetIntervalMixin {
    private static final int FRMC$FLY_SCAN_INTERVAL = 10;

    @ModifyArg(method = "onEntityFinalizeSpawn", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal;<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V"),
            index = 2, require = 0)
    private int frmc$throttleFlyScan(Mob mob, Class<?> targetClass, int interval, boolean mustSee, boolean mustReach,
                                     Predicate<LivingEntity> predicate) {
        return targetClass == EntityFly.class && interval < FRMC$FLY_SCAN_INTERVAL ? FRMC$FLY_SCAN_INTERVAL : interval;
    }
}
