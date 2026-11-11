package com.y271727uy.FRMC.mixin.acceleratedrendering.touhoulittlemaid;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.github.tartaricacid.touhoulittlemaid.compat.sodium.SodiumCompat;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SodiumCompat.class, remap = false)
public abstract class SodiumCompatMixin {
    // The maid renderer calls this after applying the bone pose and glow light.
    @Inject(method = "sodiumRenderCubesOfBone", at = @At("HEAD"), cancellable = true, require = 1)
    private static void frmc$renderAcceleratedBone(AnimatedGeoBone bone, PoseStack poseStack, VertexConsumer buffer,
                                                   int light, int overlay, float red, float green, float blue,
                                                   float alpha, CallbackInfoReturnable<Boolean> cir) {
        if (!CoreFeature.isLoaded() || !AcceleratedEntityRenderingFeature.isEnabled()
                || !AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                || !ModsFeature.isEnabled() || !ModsFeature.shouldAccelerateTlm()
                || !(CoreFeature.isRenderingLevel()
                        || CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui())
                || !(buffer instanceof IAcceleratedVertexConsumer accelerated) || !accelerated.isAccelerated()
                || !(bone.geoBone() instanceof IAcceleratedRenderer<?> renderer)) {
            return;
        }
        if (!bone.isHidden() && !bone.cubesAreHidden()) {
            @SuppressWarnings("unchecked")
            var maidRenderer = (IAcceleratedRenderer<AnimatedGeoBone>) renderer;
            PoseStack.Pose pose = poseStack.last();
            int color = FastColor.ARGB32.color((int) (alpha * 255), (int) (red * 255),
                    (int) (green * 255), (int) (blue * 255));
            accelerated.doRender(maidRenderer, bone, pose.pose(), pose.normal(), light, overlay, color);
        }
        cir.setReturnValue(true);
    }
}
