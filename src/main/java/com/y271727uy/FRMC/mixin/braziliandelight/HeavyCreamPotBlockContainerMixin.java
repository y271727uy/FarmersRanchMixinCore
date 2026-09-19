package com.y271727uy.FRMC.mixin.braziliandelight;

import com.y271727uy.FRMC.integration.braziliandelight.ContainerStackHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.dannbrown.braziliandelight.common.content.blocks.HeavyCreamPotBlock", remap = false)
public abstract class HeavyCreamPotBlockContainerMixin {
    @Redirect(
        method = {"use", "m_6227_"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;m_21008_(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        require = 0,
        remap = false
    )
    private void frmc$preserveStackedContainer(Player player, InteractionHand hand, ItemStack replacement) {
        ContainerStackHelper.replaceStackedContainer(player, hand, replacement);
    }
}
