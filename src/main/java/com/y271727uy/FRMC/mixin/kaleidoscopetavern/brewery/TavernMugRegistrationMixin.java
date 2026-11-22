package com.y271727uy.FRMC.mixin.kaleidoscopetavern.brewery;

import com.github.ysbbbbbb.kaleidoscopetavern.KaleidoscopeTavern;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernMugs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KaleidoscopeTavern.class, remap = false)
public abstract class TavernMugRegistrationMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$registerMugs(CallbackInfo ci) {
        ModBlocks.BLOCKS.register(TavernMugs.EMPTY, TavernMugs::emptyBlock);
        ModBlocks.BLOCKS.register(TavernMugs.FILLED, TavernMugs.FilledBlock::new);
    }
}
