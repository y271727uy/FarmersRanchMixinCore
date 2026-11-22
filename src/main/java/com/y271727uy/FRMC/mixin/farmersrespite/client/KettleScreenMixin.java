package com.y271727uy.FRMC.mixin.farmersrespite.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.client.farmersrespite.FRMCKettleScreen;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import java.awt.Rectangle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import umpaz.farmersrespite.client.gui.KettleScreen;

@Mixin(value = KettleScreen.class, remap = false)
public abstract class KettleScreenMixin {
    @WrapOperation(method = {"renderBg", "m_7286_"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/fluids/capability/templates/FluidTank;getFluid()Lnet/minecraftforge/fluids/FluidStack;"),
            require = 1)
    private FluidStack frmc$renderMixedTank(FluidTank tank, Operation<FluidStack> original,
                                            @Local(argsOnly = true) GuiGraphics graphics) {
        if ((Object) this instanceof FRMCKettleScreen screen && tank instanceof KettleFluidTank mixed
                && !mixed.getFluidInTank(1).isEmpty()) {
            screen.renderStoredFluids(graphics, mixed);
            return FluidStack.EMPTY;
        }
        return original.call(tank);
    }

    @Inject(method = "renderTankTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$showStoredFluids(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object) this instanceof FRMCKettleScreen screen && screen.renderStoredFluidsTooltip(graphics, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = {"renderBg", "m_7286_"}, at = @At(value = "FIELD", target =
            "Lumpaz/farmersrespite/client/gui/KettleScreen;BACKGROUND_TEXTURE:Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation frmc$useScreenTexture(ResourceLocation original) {
        return (Object) this instanceof FRMCKettleScreen ? FRMCKettleScreen.TEXTURE : original;
    }

    @ModifyExpressionValue(method = {"renderBg", "m_7286_", "renderHeatIndicatorTooltip"},
            at = @At(value = "FIELD", target = "Lumpaz/farmersrespite/client/gui/KettleScreen;HEAT_ICON:Ljava/awt/Rectangle;"))
    private Rectangle frmc$useScreenHeatIndicator(Rectangle original) {
        return (Object) this instanceof FRMCKettleScreen screen ? screen.heatIndicator() : original;
    }
}
