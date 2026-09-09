package com.y271727uy.FRMC.mixin.minecraft.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void frmc$suppressSdvfOptionalModReminder(Component message, CallbackInfo ci) {
        if (message.getString().startsWith("[SDVF] Optional mods missing:")) {
            ci.cancel();
        }
    }
}
