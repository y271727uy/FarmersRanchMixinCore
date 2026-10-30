package com.y271727uy.frmc.mixinagent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class DeepFields {
    private DeepFields() {
    }

    public static void setStatic(Class<?> type, String fieldName, Object value) {
        if (type == null || fieldName == null) {
            throw new IllegalArgumentException("type and field are required");
        }
        ModuleOpener.open(type.getModule());
        try {
            Field field = find(type, fieldName);
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException(fieldName + " is not static");
            }
            writeStatic(field, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot write " + type.getName() + "." + fieldName, exception);
        }
    }

    public static void setStatic(String className, String fieldName, Object value) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            setStatic(type, fieldName, value);
        } catch (ClassNotFoundException exception) {
            try {
                setStatic(Class.forName(className, false, DeepFields.class.getClassLoader()), fieldName, value);
            } catch (ClassNotFoundException again) {
                throw new IllegalStateException("Class not loaded: " + className, again);
            }
        }
    }

    private static void writeStatic(Field field, Object value) throws ReflectiveOperationException {
        Object unsafe = UnsafeAccess.get();
        if (unsafe == null) {
            field.set(null, value);
            return;
        }
        Object base = unsafe.getClass().getMethod("staticFieldBase", Field.class).invoke(unsafe, field);
        long offset = (Long) unsafe.getClass().getMethod("staticFieldOffset", Field.class).invoke(unsafe, field);
        Class<?> type = field.getType();
        if (type.isPrimitive()) {
            unsafe.getClass().getMethod(putName(type), Object.class, long.class, primitiveCarrier(type)).invoke(unsafe, base, offset, value);
            return;
        }
        unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, base, offset, value);
    }

    private static Field find(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static String putName(Class<?> type) {
        if (type == int.class) return "putInt";
        if (type == long.class) return "putLong";
        if (type == boolean.class) return "putBoolean";
        if (type == byte.class) return "putByte";
        if (type == short.class) return "putShort";
        if (type == char.class) return "putChar";
        if (type == float.class) return "putFloat";
        if (type == double.class) return "putDouble";
        throw new IllegalArgumentException(type.getName());
    }

    private static Class<?> primitiveCarrier(Class<?> type) {
        if (type == int.class) return int.class;
        if (type == long.class) return long.class;
        if (type == boolean.class) return boolean.class;
        if (type == byte.class) return byte.class;
        if (type == short.class) return short.class;
        if (type == char.class) return char.class;
        if (type == float.class) return float.class;
        if (type == double.class) return double.class;
        return type;
    }
}
