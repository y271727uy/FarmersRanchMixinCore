package com.y271727uy.frmc.mixinfabricextras;

import com.y271727uy.frmc.mixinfabricextras.injector.FabricInjectInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

public final class MixinFabricExtras {
    private static boolean initialized;

    private MixinFabricExtras() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        InjectionInfo.register(FabricInjectInjectionInfo.class);
        initialized = true;
    }
}
