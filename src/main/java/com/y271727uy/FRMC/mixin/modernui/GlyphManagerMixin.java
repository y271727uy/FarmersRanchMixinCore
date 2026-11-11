package com.y271727uy.FRMC.mixin.modernui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.modernui.ModernUIFlagEmojiIntegration;
import icyllis.modernui.ModernUI;
import icyllis.modernui.mc.text.GlyphManager;
import java.io.IOException;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = GlyphManager.class, remap = false)
public abstract class GlyphManagerMixin {
    @WrapOperation(
            method = "cacheEmoji",
            at = @At(
                    value = "INVOKE",
                    target = "Licyllis/modernui/ModernUI;getResourceStream(Ljava/lang/String;Ljava/lang/String;)Ljava/io/InputStream;"
            ),
            remap = false
    )
    private InputStream frmc$openBundledEmoji(ModernUI modernUI, String namespace, String path, Operation<InputStream> original) throws IOException {
        if ("modernui".equals(namespace)) {
            InputStream stream = ModernUIFlagEmojiIntegration.openBundledEmoji(path);
            if (stream != null) {
                return stream;
            }
        }
        return original.call(modernUI, namespace, path);
    }
}
