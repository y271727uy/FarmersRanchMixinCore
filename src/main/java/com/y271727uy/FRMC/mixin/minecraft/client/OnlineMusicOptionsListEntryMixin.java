package com.y271727uy.FRMC.mixin.minecraft.client;

import com.y271727uy.FRMC.client.netmusic.OnlineMusicSettings;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Creates FRMC's list entry as a normal navigation button instead of a boolean option. */
@Mixin(OptionsList.Entry.class)
public abstract class OnlineMusicOptionsListEntryMixin {
    @Redirect(
            method = "small",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;createButton(Lnet/minecraft/client/Options;III)Lnet/minecraft/client/gui/components/AbstractWidget;")
    )
    private static AbstractWidget frmc$createOnlineMusicButton(
            OptionInstance<?> option, Options options, int x, int y, int width
    ) {
        if (OnlineMusicSettings.isSettingsOption(option)) {
            return OnlineMusicSettings.button(x, y, width);
        }
        return option.createButton(options, x, y, width);
    }
}
