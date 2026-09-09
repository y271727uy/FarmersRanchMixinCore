package com.y271727uy.FRMC.client.modelgap;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoader;

public final class ModelGapFix {
    private static final ResourceLocation BLOCK_ATLAS = ResourceLocation.fromNamespaceAndPath(
        "minecraft",
        "textures/atlas/blocks.png"
    );
    private static final boolean MAC_OS = Util.getPlatform() == Util.OS.OSX;

    private ModelGapFix() {
    }

    public static boolean isEnabled() {
        return ModLoader.isLoadingStateValid() && ModelGapFixConfig.ENABLED.get();
    }

    public static float adjustShrinkRatio(
        ResourceLocation atlasLocation,
        float defaultValue,
        float currentValue
    ) {
        if (!isEnabled() || !BLOCK_ATLAS.equals(atlasLocation) || defaultValue != currentValue) {
            return currentValue;
        }

        double multiplier = MAC_OS ? ModelGapFixConfig.MAC_SHRINK_RATIO_MULTIPLIER.get() : 0.0D;
        return (float) (defaultValue * multiplier);
    }

    public static void createOrExpandSpan(
        List<ItemModelGenerator.Span> spans,
        ItemModelGenerator.SpanFacing facing,
        int pixelX,
        int pixelY
    ) {
        double expansion = isEnabled() ? expansion() : 0.0D;
        ItemModelGenerator.Span existingSpan = null;

        for (ItemModelGenerator.Span span : spans) {
            if (span.getFacing() != facing) {
                continue;
            }

            int anchor = facing.isHorizontal() ? pixelY : pixelX;
            if (span.getAnchor() != anchor) {
                continue;
            }

            int adjacentPixel = (facing.isHorizontal() ? pixelX : pixelY) - 1;
            if (expansion != 0.0D && span.getMax() != adjacentPixel) {
                continue;
            }

            existingSpan = span;
            break;
        }

        int length = facing.isHorizontal() ? pixelX : pixelY;
        if (existingSpan == null) {
            int anchor = facing.isHorizontal() ? pixelY : pixelX;
            spans.add(new ItemModelGenerator.Span(facing, length, anchor));
        } else {
            existingSpan.expand(length);
        }
    }

    public static void enlargeFaces(List<BlockElement> elements) {
        if (!isEnabled()) {
            return;
        }

        double indent = indent();
        double expansion = expansion();
        for (BlockElement element : elements) {
            if (element.faces.size() != 1) {
                continue;
            }

            switch (element.faces.keySet().iterator().next()) {
                case UP -> {
                    element.from.set(element.from.x() - expansion, element.from.y() - indent, element.from.z() - expansion);
                    element.to.set(element.to.x() + expansion, element.to.y() - indent, element.to.z() + expansion);
                }
                case DOWN -> {
                    element.from.set(element.from.x() - expansion, element.from.y() + indent, element.from.z() - expansion);
                    element.to.set(element.to.x() + expansion, element.to.y() + indent, element.to.z() + expansion);
                }
                case WEST -> {
                    element.from.set(element.from.x() - indent, element.from.y() + expansion, element.from.z() - expansion);
                    element.to.set(element.to.x() - indent, element.to.y() - expansion, element.to.z() + expansion);
                }
                case EAST -> {
                    element.from.set(element.from.x() + indent, element.from.y() + expansion, element.from.z() - expansion);
                    element.to.set(element.to.x() + indent, element.to.y() - expansion, element.to.z() + expansion);
                }
                default -> {
                }
            }
        }
    }

    private static double expansion() {
        return MAC_OS
            ? ModelGapFixConfig.MAC_ITEM_QUADS_EXPANSION.get()
            : ModelGapFixConfig.ITEM_QUADS_EXPANSION.get();
    }

    private static double indent() {
        return MAC_OS
            ? ModelGapFixConfig.MAC_ITEM_QUADS_INDENT.get()
            : ModelGapFixConfig.ITEM_QUADS_INDENT.get();
    }
}
