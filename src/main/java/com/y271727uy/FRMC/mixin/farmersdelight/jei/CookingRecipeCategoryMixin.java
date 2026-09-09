package com.y271727uy.FRMC.mixin.farmersdelight.jei;

import com.y271727uy.FRMC.util.CookingPotLosslessContainerHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.integration.jei.category.CookingRecipeCategory", remap = false)
public abstract class CookingRecipeCategoryMixin {
    @Unique
    private static final int FRMC$CONTAINER_SLOT_X = 60;
    //private static final double FRMC$CONTAINER_SLOT_X = 60.5;
    @Unique
    private static final int FRMC$CONTAINER_SLOT_Y = 39;
    @Unique
    private static final int FRMC$LOSSLESS_LABEL_Y = 39;
    @Unique
    private static final int FRMC$LOSSLESS_LABEL_Z = 400;
    @Unique
    private static final int FRMC$LOSSLESS_LABEL_COLOR = 0xFF5555;

    @Inject(
        method = "draw(Lvectorwing/farmersdelight/common/crafting/CookingPotRecipe;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V",
        at = @At("TAIL"),
        require = 0
    )
    private void frmc$drawLosslessContainerLabel(
            CookingPotRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY,
            CallbackInfo ci
    ) {
        ItemStack container = recipe.getOutputContainer();
        if (!CookingPotLosslessContainerHelper.isLosslessContainer(container)) {
            return;
        }

        Component label = Component.translatable("frmc.jei.cooking_pot.lossless");
        Font font = Minecraft.getInstance().font;
        int textX = Math.round((FRMC$CONTAINER_SLOT_X + 8) * 2.0F - font.width(label) / 2.0F);
        int textY = FRMC$LOSSLESS_LABEL_Y * 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, FRMC$LOSSLESS_LABEL_Z);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.drawString(font, label, textX + 1, textY + 1, 0x550000, false);
        guiGraphics.drawString(font, label, textX, textY, FRMC$LOSSLESS_LABEL_COLOR, false);
        guiGraphics.pose().popPose();
    }
}
