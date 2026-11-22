package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.crafting.serializer.BarrelRecipeSerializer;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BarrelRecipeSerializer.class, remap = false)
public abstract class BarrelRecipeSerializerMixin {
    @WrapOperation(method = "fromJson", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/util/GsonHelper;getAsString(Lcom/google/gson/JsonObject;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    private String frmc$defaultEmptyBaseToWater(JsonObject recipe, String key, String fallback,
                                                Operation<String> original) {
        if (recipe.has(key) && recipe.get(key).isJsonNull()) {
            return fallback;
        }
        String fluid = original.call(recipe, key, fallback);
        return fluid.isBlank() || fluid.equals("empty") || fluid.equals("minecraft:empty") ? fallback : fluid;
    }
}
