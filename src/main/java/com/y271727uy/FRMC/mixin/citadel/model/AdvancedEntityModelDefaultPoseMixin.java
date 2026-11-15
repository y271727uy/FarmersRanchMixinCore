package com.y271727uy.FRMC.mixin.citadel.model;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.anim.CitadelDefaultPoseParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code resetToDefaultPose} 每个实体每帧调用一次，而大多数模型的 {@code getAllParts()} 每次都新建一个列表，
 * Citadel 再用 lambda 遍历它。这里在部件列表连续几次完全相同之后改用缓存的数组重置，结果与原版一致。
 */
@Pseudo
@Mixin(value = AdvancedEntityModel.class, remap = false)
public abstract class AdvancedEntityModelDefaultPoseMixin {
    @Shadow
    public abstract Iterable<AdvancedModelBox> getAllParts();

    @Unique
    private AdvancedModelBox[] frmc$defaultPoseParts;
    @Unique
    private int frmc$stableListings;

    @Inject(method = "resetToDefaultPose", at = @At("HEAD"), cancellable = true)
    private void frmc$resetFromCachedParts(CallbackInfo callback) {
        if (!Config.citadelCachedDefaultPoseEnabled) {
            return;
        }
        AdvancedModelBox[] parts = frmc$defaultPoseParts;
        if (frmc$stableListings < CitadelDefaultPoseParts.REQUIRED_STABLE_LISTINGS) {
            AdvancedModelBox[] listed = CitadelDefaultPoseParts.list(getAllParts());
            frmc$stableListings = CitadelDefaultPoseParts.sameParts(parts, listed) ? frmc$stableListings + 1 : 1;
            frmc$defaultPoseParts = parts = listed;
        }
        callback.cancel();
        for (AdvancedModelBox part : parts) {
            part.resetToDefaultPose();
        }
    }
}
