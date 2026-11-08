package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.crafting.CottonCandyMachineRecipe;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = CottonCandyMachineRecipe.Serializer.class, remap = false)
public abstract class CottonCandyMachineRecipeSerializerMixin {
    @Inject(method = "toNetwork(Lnet/minecraft/network/FriendlyByteBuf;Lcom/cloudmeow/delightoflight/crafting/CottonCandyMachineRecipe;)V",
            at = @At("HEAD"))
    private void frmc$writeRecipeGroup(FriendlyByteBuf buffer, CottonCandyMachineRecipe recipe, CallbackInfo ci) {
        buffer.writeUtf(recipe.getGroup());
    }
}
