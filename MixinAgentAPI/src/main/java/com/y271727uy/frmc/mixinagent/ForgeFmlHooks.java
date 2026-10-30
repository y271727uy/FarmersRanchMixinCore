package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.ClassEditor;
import com.y271727uy.frmc.mixinasm.MethodEdits;

/**
 * Entry point for rewrites that have to reach Forge and FML themselves.
 *
 * <p>Register exact hooks from {@link #registerBuiltIn()}. Classes under
 * {@code net.minecraftforge.fml.loading} and {@code cpw.mods.modlauncher} are loaded
 * before ModLauncher's transforming class loader exists. The Java agent applies those
 * hooks at define time; a later {@link Substratum#retransform(String)} can still replace
 * method bodies, but cannot add or remove fields or methods.
 */
public final class ForgeFmlHooks {
    private static final String[] PLATFORM_PACKAGES = {
            "cpw.mods.modlauncher",
            "cpw.mods.bootstraplauncher",
            "net.minecraftforge.fml.loading",
            "net.minecraftforge.fml.loading.moddiscovery",
            "net.minecraftforge.common",
            "net.minecraftforge.fml"
    };

    private ForgeFmlHooks() {
    }

    public static void edit(String className, ClassEditor editor) {
        Substratum.edit(className, editor);
    }

    public static void edit(String id, String className, ClassEditor editor) {
        Substratum.edit(id, className, editor);
    }

    static void registerBuiltIn() {
        if (Boolean.getBoolean("frmc.substratum.forceFmlProduction")) {
            edit("fmlloader.production", "net.minecraftforge.fml.loading.FMLLoader",
                    node -> MethodEdits.setBooleanConstant(node, "isProduction", "()Z", true));
        }
    }

    static int openLoadedPlatformModules() {
        int opened = 0;
        java.util.Set<Module> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        java.util.Set<ModuleLayer> seenLayers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        java.util.List<ModuleLayer> layers = new java.util.ArrayList<>();
        ModuleLayer currentLayer = ForgeFmlHooks.class.getModule().getLayer();
        if (currentLayer != null) {
            layers.add(currentLayer);
        }
        layers.add(ModuleLayer.boot());
        for (int index = 0; index < layers.size(); index++) {
            ModuleLayer layer = layers.get(index);
            if (!seenLayers.add(layer)) {
                continue;
            }
            layers.addAll(layer.parents());
            for (Module module : layer.modules()) {
                for (String pkg : PLATFORM_PACKAGES) {
                    if (!module.getPackages().contains(pkg)) {
                        continue;
                    }
                    // Inspect module metadata only. Class.forName would define game
                    // classes before Mixin has finished preparing their mixins.
                    try {
                        if (seen.add(module) && ModuleOpener.open(module)) {
                            opened++;
                        }
                    } catch (Throwable ignored) {
                        // A later edit can retry opening an already-loaded module.
                    }
                    break;
                }
            }
        }
        return opened;
    }
}
