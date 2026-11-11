package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.render.entity.CorrodentRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(CorrodentRenderer.class)
public abstract class CorrodentCrackRenderMixin {
    @ModifyConstant(
            method = "render(Lcom/github/alexmodguy/alexscaves/server/entity/living/CorrodentEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.LESS_THAN_ZERO),
            slice = @Slice(
                    from = @At(value = "INVOKE", remap = false, target =
                            "Lcom/github/alexmodguy/alexscaves/server/entity/living/CorrodentEntity;getCorrosionAmount(Lnet/minecraft/core/BlockPos;)I"),
                    to = @At(value = "INVOKE", remap = false, target =
                            "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")),
            remap = false, require = 1, allow = 1)
    private int frmc$firstVisibleCrackLevel(int minimum) {
        // renderEntireBatch subtracts one before indexing the breaking textures.
        return 1;
    }
}
