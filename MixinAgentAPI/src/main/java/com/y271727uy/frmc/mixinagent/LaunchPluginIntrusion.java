package com.y271727uy.frmc.mixinagent;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.lang.reflect.Field;
import java.util.Map;

final class LaunchPluginIntrusion {
    static final String PLUGIN_NAME = "frmc_substratum";

    private LaunchPluginIntrusion() {
    }

    static boolean install(ILaunchPluginService plugin) {
        try {
            Class<?> launcherType = Class.forName("cpw.mods.modlauncher.Launcher", false, LaunchPluginIntrusion.class.getClassLoader());
            Object launcher = launcherType.getField("INSTANCE").get(null);
            if (launcher == null) {
                return false;
            }
            Object handler = read(launcher, "launchPlugins");
            return insert(handler, plugin);
        } catch (Throwable throwable) {
            return false;
        }
    }

    static Object installed() {
        try {
            Class<?> launcherType = Class.forName("cpw.mods.modlauncher.Launcher", false, LaunchPluginIntrusion.class.getClassLoader());
            Object launcher = launcherType.getField("INSTANCE").get(null);
            if (launcher == null) {
                return null;
            }
            Object handler = read(launcher, "launchPlugins");
            Object map = read(handler, "plugins");
            if (map instanceof Map<?, ?> plugins) {
                return plugins.get(PLUGIN_NAME);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean insert(Object handler, Object plugin) {
        if (handler == null || plugin == null) {
            return false;
        }
        try {
            Object map = read(handler, "plugins");
            if (!(map instanceof Map<?, ?>)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<Object, Object> plugins = (Map<Object, Object>) map;
            String name = plugin instanceof ILaunchPluginService service ? service.name() : PLUGIN_NAME;
            if (plugins.containsKey(name)) {
                return true;
            }
            plugins.put(name, plugin);
            return plugins.get(name) == plugin;
        } catch (RuntimeException exception) {
            System.err.println("[FRMC/substratum] launch plugin map rejected insert: " + exception);
            return false;
        } catch (ReflectiveOperationException exception) {
            System.err.println("[FRMC/substratum] launch plugin map unreadable: " + exception);
            return false;
        }
    }

    private static Object read(Object owner, String fieldName) throws ReflectiveOperationException {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
