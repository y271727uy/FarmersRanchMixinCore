package com.y271727uy.FRMC.client.farmersrespite;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import umpaz.farmersrespite.client.gui.KettleScreen;
import umpaz.farmersrespite.common.block.entity.container.KettleMenu;

public final class FRMCKettleScreen extends KettleScreen {
    public static final ResourceLocation TEXTURE = new ResourceLocation(FRMCMod.MODID,
            "textures/gui/farmersrespite/kettle.png");

    private final Rectangle heatIndicator = new Rectangle(32, 55, 17, 15);

    public FRMCKettleScreen(KettleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelX = inventoryLabelX;
    }

    public Rectangle heatIndicator() {
        return heatIndicator;
    }

    public void renderStoredFluids(GuiGraphics graphics, KettleFluidTank tank) {
        List<FluidStack> inputs = new ArrayList<>(tank.getTanks());
        for (int index = 0; index < tank.getTanks(); index++) {
            FluidStack stored = tank.getFluidInTank(index);
            if (!stored.isEmpty()) {
                inputs.add(stored);
            }
        }
        var segments = KettleFluidBands.layout(inputs, 28, tank.getCapacity());
        KettleFluidBands.draw(graphics, segments, leftPos + 106, topPos + 18, 38);
        graphics.blit(TEXTURE, leftPos + 105, topPos + 17, 176, 32, 40, 34);
    }

    public boolean renderStoredFluidsTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!(getMenu().kettleTank instanceof KettleFluidTank tank) || tank.getFluidInTank(1).isEmpty()
                || !isHovering(106, 18, 37, 28, mouseX, mouseY)) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        for (int index = 0; index < tank.getTanks(); index++) {
            var stored = tank.getFluidInTank(index);
            if (!stored.isEmpty()) {
                lines.add(stored.getDisplayName().copy().append(" (" + stored.getAmount() + " mB)"));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }
}
