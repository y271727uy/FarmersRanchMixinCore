package com.y271727uy.FRMC.mixin.untamed_wilds.entity.mammal;

import untamedwilds.entity.ComplexMob;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "untamedwilds.entity.mammal.EntityBigCat", remap = false)
public abstract class EntityBigCatTextureCacheMixin {
    @Unique
    private ResourceLocation frmc$cachedTexture;
    @Unique
    private int frmc$cachedSkin = Integer.MIN_VALUE;
    @Unique
    private int frmc$cachedVariant = Integer.MIN_VALUE;
    @Unique
    private int frmc$cachedGender = Integer.MIN_VALUE;

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void frmc$useCachedTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cachedTexture = this.frmc$cachedTexture;
        if (cachedTexture == null) {
            return;
        }

        ComplexMob complexMob = (ComplexMob) (Object) this;
        int skin = complexMob.getSkin();
        int variant = complexMob.getVariant();
        String genderString = this.frmc$getGenderString();
        int genderHash = genderString.hashCode();
        if (this.frmc$cachedSkin != skin || this.frmc$cachedVariant != variant || this.frmc$cachedGender != genderHash) {
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

        ComplexMob complexMob = (ComplexMob) (Object) this;
        String genderString = this.frmc$getGenderString();
        this.frmc$cachedTexture = texture;
        this.frmc$cachedSkin = complexMob.getSkin();
        this.frmc$cachedVariant = complexMob.getVariant();
        this.frmc$cachedGender = genderString.hashCode();
    }

    @Unique
    private String frmc$getGenderString() {
        try {
            Method method = this.getClass().getMethod("getGenderString");
            Object value = method.invoke(this);
            if (value instanceof String genderString) {
                return genderString;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return "unknown";
    }
}




