package com.y271727uy.FRMC.mixin.vinery.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.capability.displaywine.client.DisplayWineBottleRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.satisfy.vinery.client.render.block.storage.WineBottleRenderer;
import net.satisfy.vinery.core.block.entity.StorageBlockEntity;
import net.satisfy.vinery.core.item.DrinkBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = WineBottleRenderer.class, remap = false)
public abstract class WineBottleRendererMixin {
    @Unique
    private static final DisplayWineBottleRenderer frmc$renderer = new DisplayWineBottleRenderer();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void frmc$renderForeignBottles(
            StorageBlockEntity entity,
            PoseStack pose,
            MultiBufferSource buffer,
            NonNullList<ItemStack> stacks,
            CallbackInfo ci) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && !(stack.getItem() instanceof DrinkBlockItem) && stack.getItem() instanceof BlockItem) {
                frmc$renderer.render(entity, pose, buffer, stacks);
                ci.cancel();
                return;
            }
        }
    }
}
