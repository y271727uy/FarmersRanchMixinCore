package com.y271727uy.FRMC.integration.oculus;

import net.minecraftforge.fml.loading.LoadingModList;

public class OculusIntegration {
    private OculusIntegration() {
    }

    /** Oculus keeps Iris' package names but exposes the Forge mod id "oculus". */
    public static final boolean IS_IRIS_INSTALLED =
            LoadingModList.get().getModFileById("oculus") != null
                    || LoadingModList.get().getModFileById("iris") != null;

    /**
     * Mixin classes must call this instead of IrisApi directly. While preparing a mixin, Mixin resolves the
     * owner of every method call in it, so a mixin that mentions IrisApi fails to apply (and takes the target
     * class with it) when Oculus is absent, even behind an IS_IRIS_INSTALLED check. Ordinary classes like this
     * one only resolve IrisApi when the guarded line actually runs.
     */
    public static boolean isShaderPackInUse() {
        return IS_IRIS_INSTALLED && IrisState.shaderPackInUse();
    }

    private static final class IrisState {
        private static boolean shaderPackInUse() {
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
