package com.y271727uy.FRMC.mixin.mystiasizakaya;

import net.minecraftforge.client.event.RenderGuiEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.hiedacamellia.mystiasizakaya.content.client.overlay.OrdersOverlay", remap = false)
public abstract class OrdersOverlayMixin {
    private static final int FRMC$ORIGINAL_OVERLAY_CENTER_X = 156;
    private static final int FRMC$ORDER_OVERLAY_X_OFFSET = -110;
    private static final int FRMC$ORDER_OVERLAY_Y_OFFSET = 2;

    @Unique
    private static int FRMC$currentGuiWidth = FRMC$ORIGINAL_OVERLAY_CENTER_X * 2;

    @Inject(method = "eventHandler", at = @At("HEAD"), require = 0, expect = 0)
    private static void frmc$captureGuiWidth(RenderGuiEvent.Pre event, CallbackInfo ci) {
        FRMC$currentGuiWidth = event.getGuiGraphics().guiWidth();
    }

    @ModifyArg(
        method = "eventHandler",
        at = @At(
            value = "INVOKE",
            target = "Lorg/hiedacamellia/mystiasizakaya/content/client/overlay/OrdersOverlay;renderPart(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 1,
        require = 0,
        expect = 0
    )
    private static int frmc$moveOrderOverlayHorizontally(int x) {
        int centeredX = FRMC$currentGuiWidth / 2 + (x - FRMC$ORIGINAL_OVERLAY_CENTER_X);
        return Math.max(4, centeredX + FRMC$ORDER_OVERLAY_X_OFFSET);
    }

    @ModifyArg(
        method = "eventHandler",
        at = @At(
            value = "INVOKE",
            target = "Lorg/hiedacamellia/mystiasizakaya/content/client/overlay/OrdersOverlay;renderPart(Lnet/minecraft/client/gui/GuiGraphics;IIILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"
        ),
        index = 2,
        require = 0,
        expect = 0
    )
    private static int frmc$moveOrderOverlayUp(int y) {
        return Math.max(4, y - FRMC$ORDER_OVERLAY_Y_OFFSET);
    }
}
