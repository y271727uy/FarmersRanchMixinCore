package com.y271727uy.FRMC.integration.brewinandchewin;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface KegWorldPouring {
    List<ItemStack> frmc$pourIntoKeg(ItemStack input, int limit, boolean creative);

    boolean frmc$canTakeFromKeg(ItemStack container);
}
