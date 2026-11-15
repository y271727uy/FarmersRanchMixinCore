package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.item.ItemWithHoverAnimation;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** 替代 Citadel ClientProxy 的悬停动画 Map<ItemStack, Float>：无悬停时 tick 直接返回，不再装箱。 */
public final class HoverAnimationState {
    private static final Reference2FloatOpenHashMap<ItemStack> PREVIOUS = newMap();
    private static final Reference2FloatOpenHashMap<ItemStack> CURRENT = newMap();

    private static final float DEFAULT_MAX_TIME = 5.0F;

    private HoverAnimationState() {}

    private static Reference2FloatOpenHashMap<ItemStack> newMap() {
        Reference2FloatOpenHashMap<ItemStack> map = new Reference2FloatOpenHashMap<>(8);
        map.defaultReturnValue(0.0F);
        return map;
    }

    public static void tick(ItemStack hovered) {
        if (CURRENT.isEmpty() && hovered == null) {
            if (!PREVIOUS.isEmpty()) {
                PREVIOUS.clear();
            }
            return;
        }

        PREVIOUS.clear();
        PREVIOUS.putAll(CURRENT);

        if (hovered != null) {
            float prev = CURRENT.getFloat(hovered);
            float maxTime = maxHoverTime(hovered);
            if (prev < maxTime) {
                CURRENT.put(hovered, prev + 1.0F);
            }
        }

        if (!CURRENT.isEmpty()) {
            ObjectIterator<Reference2FloatMap.Entry<ItemStack>> it = CURRENT.reference2FloatEntrySet().fastIterator();
            while (it.hasNext()) {
                Reference2FloatMap.Entry<ItemStack> entry = it.next();
                if (entry.getKey() == hovered) {
                    continue;
                }
                float progress = entry.getFloatValue();
                if (progress == 0.0F) {
                    it.remove();
                } else {
                    entry.setValue(progress - 1.0F);
                }
            }
        }
    }

    public static float progress(ItemStack stack) {
        float prev = PREVIOUS.getFloat(stack);
        float current = CURRENT.getFloat(stack);
        float lerped = prev + (current - prev) * Minecraft.getInstance().getFrameTime();
        return lerped / maxHoverTime(stack);
    }

    private static float maxHoverTime(ItemStack stack) {
        return stack.getItem() instanceof ItemWithHoverAnimation hoverOver
                ? hoverOver.getMaxHoverOverTime(stack)
                : DEFAULT_MAX_TIME;
    }

    public static void clearAll() {
        PREVIOUS.clear();
        CURRENT.clear();
    }
}
