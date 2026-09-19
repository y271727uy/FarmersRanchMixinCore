package com.y271727uy.FRMC.capability.displaywine;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * {@link DisplayWineTags#WINE_BOTTLE} 物品非潜行右键方块时，禁止物品自身的放置回退
 * （onItemUseFirst / useOn），避免点酒柜背面/顶面时被放到后面，以及客户端预测闪烁。
 * 方块自身的 use 不受影响。标签为空时 no-op。
 */
public final class DisplayWinePlacementHandler {
    private DisplayWinePlacementHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(DisplayWineTags.WINE_BOTTLE)) {
            return;
        }
        Player player = event.getEntity();
        if (player.isCrouching()) {
            return;
        }
        event.setUseItem(Event.Result.DENY);
    }
}
