package com.y271727uy.FRMC.mixin.manors_bounty_machine;

import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.zhaiji.manorsbountymachine.compat.farmersdelight.CookingPotRecipeCompat", remap = false)
public class CookingPotRecipeCompatMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void frmc$disableFarmersDelightCookingPotCopy(RecipeManager recipeManager, CallbackInfo ci) {
        ci.cancel();
    }
}
