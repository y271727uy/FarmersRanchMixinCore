package com.y271727uy.FRMC.mixin.extradelight;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.vomiter.extradelight.common.items.corn.ShuckableCorn", remap = false)
public abstract class ShuckableCornTooltipMixin {
    @Inject(method = {"appendHoverText", "m_7373_"}, at = @At("HEAD"), cancellable = true, require = 0, expect = 0, remap = false)
    @Dynamic
    private void frmc$removeRedundantTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        ci.cancel();
    }
}
