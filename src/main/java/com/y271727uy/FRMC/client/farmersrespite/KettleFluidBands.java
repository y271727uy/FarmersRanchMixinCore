package com.y271727uy.FRMC.client.farmersrespite;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class KettleFluidBands {
    private KettleFluidBands() {
    }

    public static List<Segment> layout(List<FluidStack> inputs, int height, int capacity) {
        long total = inputs.stream().mapToLong(FluidStack::getAmount).sum();
        if (total == 0) {
            return List.of();
        }
        int filled = Math.max(inputs.size(), (int) Math.min(height, (height * total + capacity / 2) / capacity));
        int distributedPixels = filled - inputs.size();
        List<Segment> bands = new ArrayList<>(inputs.size());
        long cumulative = 0;
        int previous = 0;
        for (FluidStack input : inputs) {
            cumulative += input.getAmount();
            // Reserve one pixel for each fluid, then distribute the rest by amount.
            int boundary = bands.size() + 1 + (int) (distributedPixels * cumulative / total);
            bands.add(new Segment(input, height - boundary, boundary - previous));
            previous = boundary;
        }
        return bands;
    }

    public static void draw(GuiGraphics graphics, List<Segment> segments, int x, int y, int width) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for (Segment segment : segments) {
            FluidStack fluid = segment.fluid();
            var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
            var texture = extension.getStillTexture(fluid);
            var sprite = texture == null ? null : atlas.apply(texture);
            int tint = extension.getTintColor(fluid);
            int top = y + segment.top();
            if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                graphics.fill(x, top, x + width, top + segment.height(), tint | 0xFF000000);
                continue;
            }
            RenderSystem.enableBlend();
            try {
                graphics.blit(x, top, 0, width, segment.height(), sprite,
                        (tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1F);
            } finally {
                RenderSystem.disableBlend();
            }
        }
    }

    public record Segment(FluidStack fluid, int top, int height) {
    }
}
