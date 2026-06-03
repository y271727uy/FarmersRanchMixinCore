package com.y271727uy.FRMC.mixin.untamed_wilds.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "untamedwilds.entity.ai.SmartLookAtGoal", remap = false)
public abstract class SmartLookAtGoalMixin extends Goal {
    @Unique
    private static final int FRMC$SCAN_STRIDE = 20;
    @Unique
    private int frmc$scanTicker = FRMC$SCAN_STRIDE;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void frmc$throttleLookAtScan(CallbackInfoReturnable<Boolean> cir) {
        if (--this.frmc$scanTicker > 0) {
            cir.setReturnValue(false);
            return;
        }

        this.frmc$scanTicker = FRMC$SCAN_STRIDE;
    }
}


