package com.y271727uy.FRMC.mixin.modernui;

import com.y271727uy.FRMC.compat.modernui.ModernUIFlagEmojiCompat;
import icyllis.modernui.mc.FontResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = FontResourceManager.class, remap = false)
public abstract class FontResourceManagerMixin {
    @Inject(method = "loadEmojis", at = @At("RETURN"), remap = false)
    private static void frmc$registerFlagEmojis(ResourceManager resourceManager,
                                                FontResourceManager.LoadResults loadResults,
                                                CallbackInfo ci) {
        ModernUIFlagEmojiCompat.registerFlagEmojis(loadResults.mEmojiFont);
    }
}
