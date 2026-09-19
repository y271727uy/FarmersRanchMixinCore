package com.y271727uy.FRMC.capability.displaywine;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import net.minecraft.world.item.ItemStack;

/**
 * 酒馆 {@code getBottleBlock} 只认 {@code BottleBlockItem}。外来酒瓶用白兰地/空瓶占位，
 * 让柜体能收下，再靠渲染 mixin 画真实模型。
 * <p>
 * 占位硬编码很脆：1.2.0 还有 {@code ModBlocks.BRANDY} / {@code EMPTY_BOTTLE}，
 * 升级酒馆时要核对这两个 RegistryObject 还在。
 */
public final class DisplayWinePlaceholders {
    public static BottleBlock placeholder(ItemStack stack) {
        if (DisplayWineBottles.isLargeBottle(stack)) {
            return (BottleBlock) ModBlocks.BRANDY.get();
        }
        return (BottleBlock) ModBlocks.EMPTY_BOTTLE.get();
    }

    private DisplayWinePlaceholders() {
    }
}
