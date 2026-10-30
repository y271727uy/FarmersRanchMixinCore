package com.y271727uy.frmc.mixinagent;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeFmlHooksTest {
    @Test
    void openingPlatformModulesDoesNotRequestPlatformClasses() throws Exception {
        String hookName = ForgeFmlHooks.class.getName();
        byte[] hookBytes;
        try (InputStream stream = ForgeFmlHooks.class.getResourceAsStream("ForgeFmlHooks.class")) {
            assertNotNull(stream);
            hookBytes = stream.readAllBytes();
        }
        List<String> requested = new ArrayList<>();
        ClassLoader loader = new ClassLoader(ForgeFmlHooks.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraftforge.") || name.startsWith("cpw.mods.")) {
                    requested.add(name);
                    throw new ClassNotFoundException(name);
                }
                if (!name.equals(hookName)) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        type = defineClass(name, hookBytes, 0, hookBytes.length);
                    }
                    if (resolve) {
                        resolveClass(type);
                    }
                    return type;
                }
            }
        };
        Class<?> isolatedHooks = loader.loadClass(hookName);
        var open = isolatedHooks.getDeclaredMethod("openLoadedPlatformModules");
        open.setAccessible(true);
        open.invoke(null);
        assertTrue(requested.isEmpty(), () -> "Module discovery requested platform classes: " + requested);
    }
}
