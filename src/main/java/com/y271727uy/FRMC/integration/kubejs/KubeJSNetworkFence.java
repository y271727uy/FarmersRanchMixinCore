package com.y271727uy.FRMC.integration.kubejs;

public final class KubeJSNetworkFence {
    private KubeJSNetworkFence() {
    }

    public static boolean isBlocked(String className) {
        return false;
    }

    public static boolean allow(boolean original, String className) {
        return original;
    }
}
