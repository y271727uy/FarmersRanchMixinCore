package com.y271727uy.frmc.mixinagent;

import java.lang.ModuleLayer;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

final class UnsafeAccess {
    private static final Object UNSAFE = load();

    private UnsafeAccess() {
    }

    static Object get() {
        return UNSAFE;
    }

    static MethodHandles.Lookup trustedLookup() {
        Object unsafe = UNSAFE;
        if (unsafe == null) {
            return null;
        }
        try {
            Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = unsafe.getClass().getMethod("staticFieldBase", Field.class).invoke(unsafe, implLookup);
            long offset = (Long) unsafe.getClass().getMethod("staticFieldOffset", Field.class).invoke(unsafe, implLookup);
            return (MethodHandles.Lookup) unsafe.getClass()
                    .getMethod("getObject", Object.class, long.class)
                    .invoke(unsafe, base, offset);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static Object load() {
        try {
            Module self = UnsafeAccess.class.getModule();
            ModuleLayer.boot().findModule("jdk.unsupported").ifPresent(self::addReads);
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field field = type.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
