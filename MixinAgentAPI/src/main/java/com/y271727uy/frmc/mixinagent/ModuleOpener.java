package com.y271727uy.frmc.mixinagent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ModuleOpener {
    private ModuleOpener() {
    }

    public static boolean open(Module module) {
        if (module == null || !module.isNamed()) {
            return true;
        }
        boolean opened = false;
        for (String pkg : module.getPackages()) {
            opened |= openPackage(module, pkg);
        }
        return opened || module.getPackages().isEmpty();
    }

    public static boolean openPackage(Module module, String pkg) {
        if (module == null || pkg == null || pkg.isEmpty()) {
            return false;
        }
        if (!module.isNamed() || module.isOpen(pkg)) {
            return true;
        }
        MethodHandles.Lookup lookup = UnsafeAccess.trustedLookup();
        if (lookup == null) {
            return false;
        }
        try {
            MethodHandle handle = lookup.findVirtual(
                    Module.class,
                    "implAddOpensToAllUnnamed",
                    MethodType.methodType(void.class, String.class));
            handle.invoke(module, pkg);
            return module.isOpen(pkg) || module.isOpen(pkg, ModuleOpener.class.getModule());
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static boolean openClass(String className) {
        try {
            Class<?> type = Class.forName(className, false, ModuleOpener.class.getClassLoader());
            return open(type.getModule());
        } catch (Throwable throwable) {
            return false;
        }
    }
}
