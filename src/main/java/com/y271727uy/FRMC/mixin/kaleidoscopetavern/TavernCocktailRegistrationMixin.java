package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.KaleidoscopeTavern;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.GlasswareBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KaleidoscopeTavern.class, remap = false)
public abstract class TavernCocktailRegistrationMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$registerCocktailDisplay(CallbackInfo ci) {
        // DrinkBlock needs two count values, but this display has no item to stack.
        ModBlocks.BLOCKS.register(TavernBarrelWine.COCKTAIL_DISPLAY.getPath(),
            () -> new DrinkBlock(2, GlasswareBlock.SHAPE, GlasswareBlock.SHAPE));
    }
}
