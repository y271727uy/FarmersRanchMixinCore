package com.y271727uy.FRMC.mixin.untamed_wilds.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "untamedwilds.entity.ai.FindItemsGoal", remap = false)
public abstract class FindItemsGoalMixin extends Goal {
    @Shadow
    @Dynamic
    private ItemEntity targetItem;

    @Shadow
    @Dynamic
    private net.minecraft.world.food.FoodProperties targetItemStack;


    // Prevent crashes when the goal starts with an invalid item target.
    @Inject(method = "m_8056_", at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
    @Dynamic
    @Unique
    private void frmc$guardStart(CallbackInfo ci) {
        if (this.frmc$isTargetValid()) {
            return;
        }

        this.frmc$clearTarget();
        ci.cancel();
    }

    // Reject targets whose FoodProperties were resolved as null, preventing follow-up NPEs in start/tick.
    @Inject(method = "m_8036_", at = @At("RETURN"), cancellable = true, require = 0, expect = 0, remap = false)
    @Dynamic
    @Unique
    private void frmc$validateChosenTarget(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        if (this.targetItemStack != null) {
            return;
        }

        this.frmc$clearTarget();
        cir.setReturnValue(false);
    }

    // Prevent crashes when the goal ticks with an invalid item target.
    @Inject(method = "m_8037_", at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
    @Dynamic
    @Unique
    private void frmc$guardTick(CallbackInfo ci) {
        if (this.frmc$isTargetValid()) {
            return;
        }

        this.frmc$clearTarget();
        ci.cancel();
    }

    @Unique
    private boolean frmc$isTargetValid() {
        return this.targetItem != null && this.targetItem.isAlive() && this.targetItemStack != null;
    }

    @Unique
    private void frmc$clearTarget() {
        this.targetItem = null;
        this.targetItemStack = null;
    }

    // Prevent crashes when the goal is queried for continuation with an invalid target.
    @Inject(method = "m_8045_", at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
    @Dynamic
    @Unique
    private void frmc$guardContinue(CallbackInfoReturnable<Boolean> cir) {
        if (this.frmc$isTargetValid()) {
            return;
        }

        this.frmc$clearTarget();
        cir.setReturnValue(false);
    }
}

