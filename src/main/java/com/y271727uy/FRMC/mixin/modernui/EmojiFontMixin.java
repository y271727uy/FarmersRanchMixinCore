package com.y271727uy.FRMC.mixin.modernui;

import com.y271727uy.FRMC.compat.modernui.EmojiFontExtension;
import com.y271727uy.FRMC.compat.modernui.ModernUIFlagEmojiCompat;
import icyllis.modernui.graphics.text.EmojiFont;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = EmojiFont.class, remap = false)
public abstract class EmojiFontMixin implements EmojiFontExtension {
    @Shadow
    @Final
    private IntSet mCoverage;

    @Shadow
    @Final
    private Object2IntMap<CharSequence> mMap;

    @Shadow
    @Final
    private List<String> mFiles;

    @Override
    public void frmc$registerEmoji(String sequence, String fileName, int[] codePoints) {
        synchronized (this.mFiles) {
            for (int codePoint : codePoints) {
                this.mCoverage.add(codePoint);
            }
            if (this.mMap.containsKey(sequence)) {
                return;
            }
            this.mFiles.add(fileName);
            this.mMap.put(sequence, this.mFiles.size());
        }
    }

    @Inject(method = "find", at = @At("RETURN"), remap = false)
    private void frmc$recordFlagLookup(char[] text, int start, int limit, CallbackInfoReturnable<Integer> cir) {
        ModernUIFlagEmojiCompat.recordFlagLookup(text, start, limit, cir.getReturnValueI());
    }
}
