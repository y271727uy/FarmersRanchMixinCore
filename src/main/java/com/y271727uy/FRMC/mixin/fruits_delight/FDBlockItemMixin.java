package com.y271727uy.FRMC.mixin.fruits_delight;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "dev.xkmc.fruitsdelight.content.item.FDBlockItem", remap = false)
public abstract class FDBlockItemMixin {
    @ModifyVariable(
            method = "finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "STORE", ordinal = 0),
            index = 4,
            require = 0,
            expect = 0,
            remap = false
    )
    private ItemStack frmc$replaceJellyRemainder(ItemStack remainder, ItemStack stack, Level level, LivingEntity consumer) {
        if (remainder.isEmpty() || !remainder.is(Items.GLASS_BOTTLE) || !frmc$isFruitsDelightJelly(stack)) {
            return remainder;
        }

        return frmc$resolveMasonJarRemainder(remainder.getCount());
    }

    @Unique
    private static boolean frmc$isFruitsDelightJelly(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && "fruitsdelight".equals(itemId.getNamespace()) && itemId.getPath().endsWith("_jelly");
    }

    @Unique
    private static ItemStack frmc$resolveMasonJarRemainder(int count) {
        ResourceLocation masonJarId = ResourceLocation.tryParse("vintagedelight:mason_jar");
        if (masonJarId == null) {
            return ItemStack.EMPTY;
        }

        Item masonJar = ForgeRegistries.ITEMS.getValue(masonJarId);
        if (masonJar == null || masonJar == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(masonJar, Math.max(count, 1));
    }
}


