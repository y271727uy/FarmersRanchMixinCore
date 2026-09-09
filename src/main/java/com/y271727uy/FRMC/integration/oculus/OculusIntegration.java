package com.y271727uy.FRMC.integration.oculus;

import net.minecraftforge.fml.loading.LoadingModList;

public class OculusIntegration {
    private OculusIntegration() {
    }

    /** Oculus keeps Iris' package names but exposes the Forge mod id "oculus". */
    public static final boolean IS_IRIS_INSTALLED =
            LoadingModList.get().getModFileById("oculus") != null
                    || LoadingModList.get().getModFileById("iris") != null;
}
