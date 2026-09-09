package com.y271727uy.FRMC.mixin.minecraft.client;

import com.y271727uy.FRMC.client.netmusic.OnlineMusicSettings;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.SimpleOptionsSubScreen;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

@Mixin(SimpleOptionsSubScreen.class)
public abstract class OnlineMusicAccessibilityMixin {
    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"),
            index = 0
    )
    private OptionInstance<?>[] frmc$appendOnlineMusicSetting(OptionInstance<?>[] options) {
        if (!((Object) this instanceof AccessibilityOptionsScreen)) {
            return options;
        }
        OptionInstance<?>[] expanded = Arrays.copyOf(options, options.length + 1);
        expanded[options.length] = OnlineMusicSettings.option();
        return expanded;
    }
}
