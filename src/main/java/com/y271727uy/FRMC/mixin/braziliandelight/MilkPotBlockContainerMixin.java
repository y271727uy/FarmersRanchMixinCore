package com.y271727uy.FRMC.mixin.braziliandelight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.braziliandelight.ContainerStackHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.dannbrown.braziliandelight.common.content.blocks.MilkPotBlock", remap = false)
public abstract class MilkPotBlockContainerMixin {
    @WrapOperation(
        method = {"use", "m_6227_"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;m_21008_(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        require = 0,
        remap = false
    )
    private void frmc$preserveStackedContainer(Player player, InteractionHand hand, ItemStack replacement, Operation<Void> original) {
        ContainerStackHelper.replaceStackedContainer(player, hand, replacement);
    }
}
