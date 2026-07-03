package com.y271727uy.FRMC.mixin.minecraft.client.diagnostics;

import com.y271727uy.FRMC.opengl.OpenGlDiagnostics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftFrameDiagnosticsMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void frmc$beginFramePhaseBreakdown(boolean renderLevel, CallbackInfo ci) {
        OpenGlDiagnostics.beginFramePhaseBreakdown();
    }

    @Inject(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundManager;updateSource(Lnet/minecraft/client/Camera;)V",
            shift = At.Shift.AFTER
        )
    )
    private void frmc$markAfterSound(boolean renderLevel, CallbackInfo ci) {
        OpenGlDiagnostics.markFramePhase("gameRenderer");
    }

    @Inject(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V",
            shift = At.Shift.AFTER
        )
    )
    private void frmc$markAfterGameRenderer(boolean renderLevel, CallbackInfo ci) {
        OpenGlDiagnostics.markFramePhase("blit");
    }

    @Inject(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V",
            shift = At.Shift.AFTER
        )
    )
    private void frmc$markAfterBlit(boolean renderLevel, CallbackInfo ci) {
        OpenGlDiagnostics.markFramePhase("updateDisplay");
    }

    @Inject(
        method = "runTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/FrameTimer;logFrameDuration(J)V",
            shift = At.Shift.AFTER
        )
    )
    private void frmc$finishFrameStats(boolean renderLevel, CallbackInfo ci) {
        OpenGlDiagnostics.finishFramePhaseBreakdown();
        OpenGlDiagnostics.logFrameTimerSummary();
        OpenGlDiagnostics.logGpuMemorySummary();
    }
}
