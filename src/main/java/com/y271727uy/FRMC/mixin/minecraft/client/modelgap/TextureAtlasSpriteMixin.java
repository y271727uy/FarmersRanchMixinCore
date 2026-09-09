package com.y271727uy.FRMC.mixin.minecraft.client.modelgap;

import com.y271727uy.FRMC.client.modelgap.ModelGapFix;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin {
    @Shadow
    protected abstract float atlasSize();

    @Shadow
    public abstract ResourceLocation atlasLocation();

    @Inject(method = "uvShrinkRatio", at = @At("RETURN"), cancellable = true)
    private void frmc$adjustUvShrinkRatio(CallbackInfoReturnable<Float> cir) {
        float vanillaRatio = 4.0F / atlasSize();
        cir.setReturnValue(ModelGapFix.adjustShrinkRatio(atlasLocation(), vanillaRatio, cir.getReturnValueF()));
    }
}
