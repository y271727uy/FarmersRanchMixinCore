package com.y271727uy.FRMC.mixin.touhoulittlemaid;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityChairRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IGeoRenderer.class, remap = false)
public interface IGeoRendererCullMixin {
    @FabricInject(method = "renderRecursively", at = @At("HEAD"), cancellable = true, remap = false)
    default void frmc$skipEmptyBone(AnimatedGeoBone bone, PoseStack pose, VertexConsumer buffer,
                                    int light, int overlay, float red, float green, float blue, float alpha,
                                    CallbackInfo ci) {
        Class<?> rendererType = getClass();
        if ((rendererType == GeckoEntityMaidRenderer.class || rendererType == GeckoEntityChairRenderer.class)
                && (bone.children().isEmpty() || bone.childBonesAreHiddenToo())
                && (bone.isHidden() || bone.cubesAreHidden() || bone.geoBone().cubes().getCubeCount() == 0)) {
            ci.cancel();
        }
    }
}
