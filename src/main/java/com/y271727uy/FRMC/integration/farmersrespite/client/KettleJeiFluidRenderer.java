package com.y271727uy.FRMC.integration.farmersrespite.client;

import com.y271727uy.FRMC.client.farmersrespite.KettleFluidBands;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

public final class KettleJeiFluidRenderer implements IIngredientRenderer<FluidStack> {
    private static final int KETTLE_CAPACITY = 1000;

    private final int width;
    private final int height;
    private final List<KettleFluidBands.Segment> segments;
    private final List<Component> tooltip;

    public KettleJeiFluidRenderer(FluidStack primary, List<FluidStack> extra, int width, int height) {
        this.width = width;
        this.height = height;
        List<FluidStack> inputs = new ArrayList<>(extra.size() + 1);
        inputs.add(primary);
        inputs.addAll(extra);

        segments = KettleFluidBands.layout(inputs, height, KETTLE_CAPACITY);
        tooltip = inputs.stream().<Component>map(input ->
                input.getDisplayName().copy().append(" (" + input.getAmount() + " mB)")).toList();
    }

    @Override
    public void render(GuiGraphics graphics, FluidStack ignored) {
        KettleFluidBands.draw(graphics, segments, 0, 0, width);
    }

    @Override
    public List<Component> getTooltip(FluidStack ignored, TooltipFlag flag) {
        return tooltip;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
