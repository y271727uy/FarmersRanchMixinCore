package com.y271727uy.FRMC.mixin.alexsmobs;

import com.y271727uy.FRMC.integration.alexsmobs.CreeperPredatorTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gates the snow leopard / tiger avoid goals Alex's Mobs adds to creepers. The goal object stays the
 * vanilla {@link AvoidEntityGoal}, so mods looking goals up by type still find it. When no tracked
 * predator is loaded, the original nearest-entity scan cannot return anything, so returning
 * {@code false} early is exact and also skips the flee path request.
 */
@Mixin(AvoidEntityGoal.class)
public abstract class CreeperPredatorAvoidGateMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    protected Class<T> avoidClass;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void frmc$skipAbsentPredators(CallbackInfoReturnable<Boolean> cir) {
        if (CreeperPredatorTracker.canSkip(this.avoidClass, this.mob.level())) {
            cir.setReturnValue(false);
        }
    }
}
