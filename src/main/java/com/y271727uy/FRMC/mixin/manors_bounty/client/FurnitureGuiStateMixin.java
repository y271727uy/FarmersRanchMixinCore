package com.y271727uy.FRMC.mixin.manors_bounty.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mcreator.manors_bounty.client.gui.CabinetGuiScreen;
import net.mcreator.manors_bounty.client.gui.DrawerGuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;

@Mixin({CabinetGuiScreen.class, DrawerGuiScreen.class})
public abstract class FurnitureGuiStateMixin {
    @WrapOperation(
            method = {"init()V", "m_7856_()V"},
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ljava/util/HashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 1
    )
    private Object frmc$keepProfileButtonsOnScreen(HashMap<?, ?> state, Object key, Object button,
            Operation<Object> original) {
        if ("button:imagebutton_profiles_button".equals(key) || "button:imagebutton_profiles_button1".equals(key)) {
            // The screen already owns these widgets. The static table would retain the screen and its level.
            return null;
        }
        return original.call(state, key, button);
    }
}
