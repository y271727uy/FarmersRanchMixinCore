package com.y271727uy.FRMC.integration.braziliandelight;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;

public final class ContainerStackHelper {
    private ContainerStackHelper() {
    }

    public static void replaceStackedContainer(Player player, InteractionHand hand, ItemStack replacement) {
        player.setItemInHand(hand, ItemUtils.createFilledResult(player.getItemInHand(hand), player, replacement));
    }
}
