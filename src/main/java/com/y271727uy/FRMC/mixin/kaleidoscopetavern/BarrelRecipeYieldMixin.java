package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.crafting.container.BarrelRecipeContainer;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.recipe.BarrelRecipe;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BarrelRecipe.class, remap = false)
public abstract class BarrelRecipeYieldMixin {
    @Inject(method = "assemble(Lcom/github/ysbbbbbb/kaleidoscopetavern/crafting/container/BarrelRecipeContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
        at = @At("RETURN"))
    private void frmc$limitConfiguredYield(BarrelRecipeContainer input, RegistryAccess registries,
                                           CallbackInfoReturnable<ItemStack> cir) {
        BarrelRecipe recipe = (BarrelRecipe) (Object) this;
        if (TavernBarrelWine.usesConfiguredYield(recipe.id())) {
            // Native assembly derives its count from the ingredients, even for liquid-only recipes.
            ItemStack output = cir.getReturnValue();
            output.setCount(Math.min(output.getCount(), recipe.result().getCount()));
        }
    }
}
