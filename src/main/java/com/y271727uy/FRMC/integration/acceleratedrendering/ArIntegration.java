package com.y271727uy.FRMC.integration.acceleratedrendering;

import net.minecraftforge.fml.loading.LoadingModList;

public final class ArIntegration {
    public static final boolean IS_INSTALLED =
            LoadingModList.get().getModFileById("acceleratedrendering") != null;

    private ArIntegration() {}
}
