package com.y271727uy.FRMC.integration.brewinandchewin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class KegJeiTankRenderer implements IIngredientRenderer<FluidStack> {
    private static final KegJeiTankRenderer INSTANCE = new KegJeiTankRenderer();
    private static final int WIDTH = 26;
    private static final int HEIGHT = 30;

    public static IRecipeSlotBuilder attachTo(IRecipeSlotBuilder slot, FluidStack fluid) {
        List<Component> lines = tankTooltip(fluid);
        return slot.setCustomRenderer(ForgeTypes.FLUID_STACK, INSTANCE)
                .addRichTooltipCallback((view, tooltip) -> {
                    if (view.getDisplayedIngredient(ForgeTypes.FLUID_STACK).isEmpty()) {
                        tooltip.addAll(lines);
                    }
                });
    }

    public static boolean isOverTank(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, FluidStack fluid) {
        // The category draws the tank from the recipe even when JEI hides the fluid ingredient.
    }

    public static void drawTank(GuiGraphics graphics, FluidStack fluid, ItemStack bottle,
                                int capacity, int x, int y) {
        if (fluid.isEmpty()) {
            return;
        }
        var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        int color = extensions.getTintColor(fluid) | 0xFF000000;
        int filled = Math.max(1, Math.min(HEIGHT, (int) (HEIGHT * (long) fluid.getAmount() / Math.max(1, capacity))));
        int top = y + HEIGHT - filled;
        var texture = extensions.getStillTexture(fluid);
        var minecraft = Minecraft.getInstance();
        var sprite = texture == null ? null : minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.fill(x, top, x + WIDTH, y + HEIGHT, color);
        if (sprite != null && !MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1F);
            RenderSystem.enableBlend();
            try {
                graphics.blit(x, top, 0, WIDTH, filled, sprite);
            } finally {
                graphics.setColor(1F, 1F, 1F, 1F);
                RenderSystem.disableBlend();
            }
        }
        if (!bottle.isEmpty()) {
            int bottleX = x + (WIDTH - 16) / 2;
            int bottleY = y + (HEIGHT - 16) / 2;
            graphics.renderItem(bottle, bottleX, bottleY);
            graphics.renderItemDecorations(minecraft.font, bottle, bottleX, bottleY);
        }
    }

    @Override
    public List<Component> getTooltip(FluidStack fluid, TooltipFlag flag) {
        return tankTooltip(fluid);
    }

    public static List<Component> tankTooltip(FluidStack fluid) {
        return fluid.isEmpty() ? List.of()
                : List.of(fluid.getDisplayName(), Component.literal(fluid.getAmount() + " mB"));
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
}
