/*
package com.y271727uy.FRMC.mixin.minecraft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceLocation.class)
public abstract class ResourceLocationMixin {
    @Unique
    private static final Logger FRMC$LOGGER = LogUtils.getLogger();

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;)V", at = @At("RETURN"))
    private void frmc$logConstructor(String namespace, String path, CallbackInfo ci) {
        FRMC$LOGGER.info("ResourceLocation created: {}:{}", namespace, path);
    }

    @Inject(method = "tryParse", at = @At("RETURN"))
    private static void frmc$logTryParse(String location, CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation result = cir.getReturnValue();
        if (result != null) {
            FRMC$LOGGER.info("ResourceLocation.tryParse('{}') -> {}", location, result);
        } else {
            FRMC$LOGGER.warn("ResourceLocation.tryParse('{}') -> null", location);
        }
    }
}

*/
//仅供调试使用