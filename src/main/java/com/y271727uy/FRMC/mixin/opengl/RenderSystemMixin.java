package com.y271727uy.FRMC.mixin.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.integration.opengl.OpenGlFrameProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * OpenGL thread and frame probe hooks on RenderSystem.
 */
@Pseudo
@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void frmc$onFlipFrameHead(long limitTime, CallbackInfo ci) {
        OpenGlFrameProbe.onFlipFrameHead();
    }

    @Inject(method = "constructThreadException", at = @At("HEAD"))
    private static void frmc$onConstructThreadException(CallbackInfoReturnable<IllegalStateException> cir) {
        OpenGlFrameProbe.onThreadViolation("RenderSystem", false);
    }
}
