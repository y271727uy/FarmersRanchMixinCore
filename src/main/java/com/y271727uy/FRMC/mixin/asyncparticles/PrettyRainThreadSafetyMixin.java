package com.y271727uy.FRMC.mixin.asyncparticles;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Pretty Rain's world-backed update callback on the client thread. */
@Pseudo
@Mixin(targets = "forge.fun.qu_an.minecraft.asyncparticles.client.AsyncTicker", remap = false)
public abstract class PrettyRainThreadSafetyMixin {
    @Inject(
        method = "addEndTickTask(Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Runnable;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void frmc$queuePrettyRainUpdate(
            ResourceLocation id, Runnable task, CallbackInfo callbackInfo) {
        if ("particlerain".equals(id.getNamespace()) && "update".equals(id.getPath())) {
            Minecraft.getInstance().execute(task);
            callbackInfo.cancel();
        }
    }
}
