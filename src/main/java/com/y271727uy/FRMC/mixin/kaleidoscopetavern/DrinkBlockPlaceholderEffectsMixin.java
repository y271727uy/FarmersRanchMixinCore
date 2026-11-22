package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DrinkBlock.class, remap = false)
public abstract class DrinkBlockPlaceholderEffectsMixin {
    @WrapOperation(method = {"onProjectileHit", "m_5581_"}, at = @At(value = "INVOKE",
        target = "Lcom/github/ysbbbbbb/kaleidoscopetavern/item/BottleBlockItem;getBrewLevel(Lnet/minecraft/world/item/ItemStack;)I"))
    private int frmc$excludePlaceholderContentsFromSplash(ItemStack stack, Operation<Integer> original) {
        // A placeholder's block type would otherwise splash the native grape wine profile.
        return TavernBarrelWine.isExternalOutput(stack) || TavernBarrelWine.hasExternalQuality(stack)
            ? 0 : original.call(stack);
    }
}
