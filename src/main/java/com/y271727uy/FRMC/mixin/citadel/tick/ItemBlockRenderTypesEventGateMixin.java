package com.y271727uy.FRMC.mixin.citadel.tick;

import com.bawnorton.mixinsquared.TargetHandler;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelRendererEvents;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 区块重建时每个流体方块都会让 Citadel post 一次 EventGetFluidRenderType；没人监听就跳过。 */
@Pseudo
@Mixin(value = ItemBlockRenderTypes.class, priority = 1500)
public abstract class ItemBlockRenderTypesEventGateMixin {
    @Dynamic("Citadel ItemBlockRenderTypes fluid layer handler")
    @TargetHandler(
            mixin = "com.github.alexthe666.citadel.mixin.client.ItemBlockRenderTypesMixin",
            name = "citadel_getFluidRenderLayer")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void frmc$gateFluidRenderLayer(
            FluidState fluidState, CallbackInfoReturnable<RenderType> original, CallbackInfo citadel) {
        if (!CitadelRendererEvents.wantsGetFluidRenderType()) {
            citadel.cancel();
        }
    }
}
