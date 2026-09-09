package com.y271727uy.FRMC.integration.vinery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class VineryGrapejuiceTooltip {
    private VineryGrapejuiceTooltip() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().hasTag() || !isGrapejuice(event.getItemStack())) {
            return;
        }

        String crusher = event.getItemStack().getTag().getString("CrusherName");
        if (!crusher.isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.frmc.vinery.crusher", crusher)
                .withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean isGrapejuice(net.minecraft.world.item.ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        return ("vinery".equals(id.getNamespace()) || "nethervinery".equals(id.getNamespace()))
            && id.getPath().contains("grapejuice");
    }
}
