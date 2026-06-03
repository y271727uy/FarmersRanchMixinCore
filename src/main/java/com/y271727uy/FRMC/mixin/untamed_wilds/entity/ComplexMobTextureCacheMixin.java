package com.y271727uy.FRMC.mixin.untamed_wilds.entity;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "untamedwilds.entity.ComplexMob", remap = false)
public abstract class ComplexMobTextureCacheMixin {
    @Unique
    private ResourceLocation frmc$cachedTexture;
    @Unique
    private int frmc$cachedSkin = Integer.MIN_VALUE;
    @Unique
    private int frmc$cachedVariant = Integer.MIN_VALUE;

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void frmc$useCachedTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cachedTexture = this.frmc$cachedTexture;
        if (cachedTexture == null) {
            return;
        }

        untamedwilds.entity.ComplexMob complexMob = (untamedwilds.entity.ComplexMob) (Object) this;
        int skin = complexMob.getSkin();
        int variant = complexMob.getVariant();
        if (this.frmc$cachedSkin != skin || this.frmc$cachedVariant != variant) {
            return;
        }

        cir.setReturnValue(cachedTexture);
    }

    @Inject(method = "getTexture", at = @At("RETURN"), require = 0, expect = 0)
    private void frmc$storeCachedTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation texture = cir.getReturnValue();
        if (texture == null) {
            return;
        }

        untamedwilds.entity.ComplexMob complexMob = (untamedwilds.entity.ComplexMob) (Object) this;
        int skin = complexMob.getSkin();
        int variant = complexMob.getVariant();
        this.frmc$cachedTexture = texture;
        this.frmc$cachedSkin = skin;
        this.frmc$cachedVariant = variant;
    }
}



