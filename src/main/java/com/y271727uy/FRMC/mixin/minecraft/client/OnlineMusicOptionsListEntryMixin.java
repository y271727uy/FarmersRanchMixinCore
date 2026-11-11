package com.y271727uy.FRMC.mixin.minecraft.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.downland.ui.AdditionalContentPackSettings;
import com.y271727uy.FRMC.client.netmusic.OnlineMusicSettings;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Creates FRMC's list entries as normal navigation buttons instead of boolean options. */
@Mixin(OptionsList.Entry.class)
public abstract class OnlineMusicOptionsListEntryMixin {
    @WrapOperation(
            method = "small",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;createButton(Lnet/minecraft/client/Options;III)Lnet/minecraft/client/gui/components/AbstractWidget;")
    )
    private static AbstractWidget frmc$createOnlineMusicButton(
            OptionInstance<?> option, Options options, int x, int y, int width, Operation<AbstractWidget> original
    ) {
        if (OnlineMusicSettings.isSettingsOption(option)) {
            return OnlineMusicSettings.button(x, y, width);
        }
        if (AdditionalContentPackSettings.isSettingsOption(option)) {
            return AdditionalContentPackSettings.button(x, y, width);
        }
        return original.call(option, options, x, y, width);
    }
}
