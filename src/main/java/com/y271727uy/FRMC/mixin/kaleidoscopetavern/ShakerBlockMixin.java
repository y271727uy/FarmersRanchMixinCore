package com.y271727uy.FRMC.mixin.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.block.mixology.ShakerBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.ShakerIngredients;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShakerBlock.class, remap = false)
public abstract class ShakerBlockMixin {
    @WrapOperation(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z", remap = true))
    private boolean frmc$allowRecipeIngredients(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original,
                                                @Local(argsOnly = true) Level level) {
        return original.call(stack, tag) || ShakerIngredients.isInRecipe(level, stack);
    }
}
