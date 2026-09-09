package com.y271727uy.FRMC.mixin.minecraft.entityactivity;

import com.y271727uy.FRMC.entity.manager.entityactivity.EntityActivityManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the entity activity manager without suppressing base entity ticks. */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void frmc$skipManagedAi(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (EntityActivityManager.shouldSkipAiStep(mob)) {
            ci.cancel();
        }
    }
}
