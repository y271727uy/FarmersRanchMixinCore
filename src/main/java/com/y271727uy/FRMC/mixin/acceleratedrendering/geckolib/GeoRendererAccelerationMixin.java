package com.y271727uy.FRMC.mixin.acceleratedrendering.geckolib;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.mods.ModsFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.frmc.mixinfabricextras.injector.FabricInject;
import net.minecraft.util.FastColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

@Mixin(value = GeoRenderer.class, remap = false)
public interface GeoRendererAccelerationMixin {
    // Run after FRMC's whole-model capture and GeckoLib's hidden-bone check.
    @FabricInject(method = "renderCubesOfBone", at = @At(value = "INVOKE",
            target = "Lsoftware/bernie/geckolib/cache/object/GeoBone;getCubes()Ljava/util/List;"),
            cancellable = true, remap = false, require = 1)
    default void frmc$renderAcceleratedBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                            int light, int overlay, float red, float green, float blue,
                                            float alpha, CallbackInfo ci) {
        if (!CoreFeature.isLoaded() || !AcceleratedEntityRenderingFeature.isEnabled()
                || !AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
                || !ModsFeature.isEnabled() || !ModsFeature.shouldAccelerateGecko()
                || !(CoreFeature.isRenderingLevel()
                        || CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui())) {
            return;
        }

        if (!(buffer instanceof IAcceleratedVertexConsumer accelerated) || !accelerated.isAccelerated()
                || !(bone instanceof IAcceleratedRenderer<?> renderer)) {
            return;
        }

        PoseStack.Pose pose = poseStack.last();
        int color = FastColor.ARGB32.color((int) (alpha * 255), (int) (red * 255),
                (int) (green * 255), (int) (blue * 255));
        accelerated.doRender(renderer, null, pose.pose(), pose.normal(), light, overlay, color);
        ci.cancel();
    }
}
