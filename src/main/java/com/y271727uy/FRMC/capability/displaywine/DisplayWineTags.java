package com.y271727uy.FRMC.capability.displaywine;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class DisplayWineTags {
    public static final TagKey<Item> SMALL_BOTTLE_WINE = item("small_bottle_wine");
    public static final TagKey<Item> LARGE_BOTTLE_WINE = item("large_bottle_wine");
    /**
     * 双瓶大酒：Kaleidoscope Tavern 的 bar_cabinet / glass_bar_cabinet
     * 允许左右各放一瓶。其余场景仍按大瓶处理。应同时加入 large_bottle_wine。
     */
    public static final TagKey<Item> DOUBLE_BOTTLE_WINE = item("double_bottle_wine");
    /**
     * 酒瓶总标签，默认包含 large / small。
     * 非潜行右键方块时禁止物品自身的放置回退；方块 use（酒柜存取）不受影响。
     */
    public static final TagKey<Item> WINE_BOTTLE = item("wine_bottle");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(FRMCMod.MODID, name));
    }

    private DisplayWineTags() {
    }
}
