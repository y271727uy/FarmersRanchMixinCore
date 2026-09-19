package com.y271727uy.FRMC.integration.kubejs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.mixin.kubejs.ScriptManagerMixin;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class KubeJSNetworkFenceTest {
    @Test
    void blocksPublishedHttpStacks() throws Exception {
        assertTrue(isBlocked("java.net.URL"));
        assertTrue(isBlocked("java.net.http.HttpClient"));
        assertTrue(isBlocked("javax.net.ssl.HttpsURLConnection"));
        assertTrue(isBlocked("org.apache.http.client.methods.HttpGet"));
        assertTrue(isBlocked("org.apache.http.impl.client.HttpClients"));
        assertTrue(isBlocked("org.apache.hc.client5.http.impl.classic.HttpClients"));
        assertTrue(isBlocked("okhttp3.OkHttpClient"));
        assertTrue(isBlocked("net.minecraft.util.HttpUtil"));
        assertTrue(isBlocked("io.netty.handler.codec.http.HttpClientCodec"));
    }

    @Test
    void blocksInnerAndSlashNames() throws Exception {
        assertTrue(isBlocked("okhttp3.OkHttpClient$Builder"));
        assertTrue(isBlocked("java/net/URL"));
    }

    @Test
    void leavesReflectionAndGameClassesAlone() throws Exception {
        assertFalse(isBlocked("java.lang.reflect.Method"));
        assertFalse(isBlocked("java.lang.reflect.Field"));
        assertFalse(isBlocked("org.apache.commons.lang3.reflect.FieldUtils"));
        assertFalse(isBlocked("java.util.ArrayList"));
        assertFalse(isBlocked("java.io.File"));
        assertFalse(isBlocked("java.nio.file.Path"));
        assertFalse(isBlocked("net.minecraft.server.level.ServerPlayer"));
        assertFalse(isBlocked("dev.latvian.mods.kubejs.recipe.schema.RecipeSchema"));
        assertFalse(isBlocked(null));
        assertFalse(isBlocked(""));
    }

    @Test
    void neverTurnsABlockedClassIntoAnAllow() throws Exception {
        assertFalse(allow(true, "org.apache.http.client.methods.HttpGet"));
        assertFalse(allow(false, "java.lang.reflect.Method"));
        assertTrue(allow(true, "java.lang.reflect.Method"));
    }

    @Test
    void decoyEntryDoesNotCarryPolicy() {
        assertFalse(KubeJSNetworkFence.isBlocked("java.net.URL"));
        assertTrue(KubeJSNetworkFence.allow(true, "java.net.URL"));
    }

    private static boolean isBlocked(String className) throws Exception {
        Method method = ScriptManagerMixin.class.getDeclaredMethod("isBlocked", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, className);
    }

    private static boolean allow(boolean original, String className) throws Exception {
        Method method = ScriptManagerMixin.class.getDeclaredMethod("allow", boolean.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, original, className);
    }
}
