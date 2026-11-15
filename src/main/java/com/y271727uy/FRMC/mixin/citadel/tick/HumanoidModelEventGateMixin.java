package com.y271727uy.FRMC.mixin.citadel.tick;

import com.bawnorton.mixinsquared.TargetHandler;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelRendererEvents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Citadel 在每个人形模型每帧的 poseRightArm/poseLeftArm 开头构造并 post EventPosePlayerHand，
 * 没有监听器时事件结果必然是默认值，直接取消 Citadel 的处理方法即可。
 */
@Pseudo
@Mixin(value = HumanoidModel.class, priority = 1500)
public abstract class HumanoidModelEventGateMixin {
    private static final String CITADEL_MIXIN = "com.github.alexthe666.citadel.mixin.client.HumanoidModelMixin";

    @Dynamic("Citadel HumanoidModel right arm handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_poseRightArm")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gatePoseRightArm(LivingEntity entity, CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsPosePlayerHand()) {
            citadel.cancel();
        }
    }

    @Dynamic("Citadel HumanoidModel left arm handler")
    @TargetHandler(mixin = CITADEL_MIXIN, name = "citadel_poseLeftArm")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gatePoseLeftArm(LivingEntity entity, CallbackInfo original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsPosePlayerHand()) {
            citadel.cancel();
        }
    }
}
