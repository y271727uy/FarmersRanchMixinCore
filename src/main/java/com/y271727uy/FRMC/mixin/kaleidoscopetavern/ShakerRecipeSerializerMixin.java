package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.crafting.serializer.ShakerRecipeSerializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.ShakerIngredients;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ShakerRecipeSerializer.class, remap = false)
public abstract class ShakerRecipeSerializerMixin {
    private static final String FROM_JSON = "fromJson(Lnet/minecraft/resources/ResourceLocation;"
            + "Lcom/google/gson/JsonObject;)Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/ShakerRecipe;";
    private static final String FROM_NETWORK = "fromNetwork(Lnet/minecraft/resources/ResourceLocation;"
            + "Lnet/minecraft/network/FriendlyByteBuf;)Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/recipe/ShakerRecipe;";

    @ModifyConstant(method = FROM_JSON, constant = @Constant(intValue = 3, ordinal = 0))
    private int frmc$sizeForJson(int original, ResourceLocation id, JsonObject json) {
        int count = GsonHelper.getAsJsonArray(json, "ingredients").size();
        if (!ShakerIngredients.validCount(count)) {
            throw new JsonSyntaxException("Shaker recipe " + id + " must have 3 to 6 ingredients, found " + count);
        }
        return count;
    }

    @ModifyConstant(method = FROM_JSON, constant = @Constant(intValue = 3, ordinal = 1))
    private int frmc$readAllJsonIngredients(int original) {
        return ShakerIngredients.MAX_INPUTS;
    }

    @WrapOperation(method = FROM_NETWORK, at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int frmc$readAllNetworkIngredients(int originalLimit, int count, Operation<Integer> original) {
        if (!ShakerIngredients.validCount(count)) {
            throw new IllegalArgumentException("Invalid shaker recipe ingredient count " + count);
        }
        return original.call(ShakerIngredients.MAX_INPUTS, count);
    }
}
