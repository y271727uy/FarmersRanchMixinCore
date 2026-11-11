package com.y271727uy.frmc.mixinfabricextras;

import com.y271727uy.frmc.mixinfabricextras.transformer.FabricInjectExtension;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;

public final class MixinFabricExtras {
    private static boolean initialized;

    private MixinFabricExtras() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        // Mixin 0.8.5 rejects registered injection annotations on interfaces during PREINJECT.
        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        Extensions extensions = (Extensions) transformer.getExtensions();
        extensions.add(new FabricInjectExtension());
        initialized = true;
    }
}
