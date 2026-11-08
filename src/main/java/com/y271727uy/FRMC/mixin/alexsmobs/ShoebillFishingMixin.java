package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityShoebill;
import com.github.alexthe666.alexsmobs.entity.ai.ShoebillAIFish;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexsmobs.BirdGroundSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ShoebillAIFish.class)
public abstract class ShoebillFishingMixin {
    @Unique private static final int FRMC$RETRY_TICKS = 10;
    @Shadow(remap = false) @Final private EntityShoebill bird;
    @Shadow(remap = false) private BlockPos waterPos;
    @Unique private BlockPos frmc$failedWater;
    @Unique private PathNavigation frmc$failedNavigator;
    @Unique private long frmc$retryAt;

    @ModifyExpressionValue(method = "generateTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;"), require = 1)
    private BlockPos frmc$skipAirAboveFishingSpot(BlockPos candidate) {
        return BirdGroundSearch.skipAir(bird.level(), candidate, 1);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$backOffAfterFailedNavigation(PathNavigation navigation, double x, double y, double z,
                                                      double speed, Operation<Boolean> original) {
        long now = bird.level().getGameTime();
        if (navigation == frmc$failedNavigator && waterPos.equals(frmc$failedWater)
                && now < frmc$retryAt && navigation.isDone()) {
            return false;
        }
        boolean moved = original.call(navigation, x, y, z, speed);
        if (!moved && navigation.isDone()) {
            frmc$failedWater = waterPos.immutable();
            frmc$failedNavigator = navigation;
            frmc$retryAt = now + FRMC$RETRY_TICKS;
        } else {
            frmc$clearFailedNavigation();
        }
        return moved;
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 1)
    private void frmc$forgetFishingRequest(CallbackInfo ci) {
        frmc$clearFailedNavigation();
    }

    @Unique
    private void frmc$clearFailedNavigation() {
        frmc$failedWater = null;
        frmc$failedNavigator = null;
    }
}
