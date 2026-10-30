package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import com.y271727uy.frmc.mixinasm.ClassEditor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

final class BridgeLink {
    private static final String BRIDGE = "com.y271727uy.frmc.mixinagent.bridge.SubstratumBridge";
    private static final String RESOURCE = "/META-INF/substratum/substratum-bridge.jar";

    private BridgeLink() {
    }

    static void install(Instrumentation instrumentation) {
        if (instrumentation == null) {
            return;
        }
        Class<?> bridge = bridgeOrNull();
        if (bridge == null) {
            bridge = defineBridge(instrumentation);
        }
        if (bridge == null) {
            return;
        }
        try {
            bridge.getMethod("bind", Object.class).invoke(null, instrumentation);
        } catch (ReflectiveOperationException exception) {
            System.err.println("[FRMC/substratum] bridge bind failed: " + exception);
        }
    }

    static boolean available() {
        return bridgeOrNull() != null;
    }

    static Instrumentation instrumentation() {
        Class<?> bridge = bridgeOrNull();
        if (bridge == null) {
            return null;
        }
        try {
            Object value = bridge.getMethod("instrumentation").invoke(null);
            return value instanceof Instrumentation instrumentation ? instrumentation : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static boolean claimTransformer() {
        Class<?> bridge = bridgeOrNull();
        if (bridge == null) {
            return true;
        }
        try {
            Object value = bridge.getMethod("claimTransformer").invoke(null);
            return Boolean.TRUE.equals(value);
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }

    static boolean publish(String id, String className, boolean prefix, ClassEditor editor) {
        Class<?> bridge = bridgeOrNull();
        if (bridge == null || id == null || editor == null) {
            return false;
        }
        try {
            Class<?> face = Class.forName(BRIDGE + "$BytesTransform", false, null);
            Object proxy = Proxy.newProxyInstance(face.getClassLoader(), new Class<?>[]{face}, handler(className, prefix, editor));
            Object added = bridge.getMethod("add", String.class, face).invoke(null, id, proxy);
            return Boolean.TRUE.equals(added);
        } catch (Throwable throwable) {
            return false;
        }
    }

    static byte[] apply(String className, byte[] bytes) {
        Class<?> bridge = bridgeOrNull();
        if (bridge == null) {
            return null;
        }
        try {
            Object value = bridge.getMethod("apply", String.class, byte[].class).invoke(null, className, bytes);
            return value instanceof byte[] rewritten ? rewritten : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static InvocationHandler handler(String className, boolean prefix, ClassEditor editor) {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(proxy, method, args);
            }
            if (!"transform".equals(method.getName()) || args == null || args.length != 2) {
                return null;
            }
            String name = Bytecodes.dotted((String) args[0]);
            byte[] bytes = (byte[]) args[1];
            boolean hit = prefix ? name.startsWith(className) : name.equals(className);
            if (!hit || bytes == null || Bytecodes.applied(bytes)) {
                return null;
            }
            try {
                return Bytecodes.edit(bytes, editor);
            } catch (Throwable throwable) {
                System.err.println("[FRMC/substratum] bridge edit failed for " + name + ": " + throwable);
                return null;
            }
        };
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "frmc-substratum-bridge";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> null;
        };
    }

    private static Class<?> defineBridge(Instrumentation instrumentation) {
        Path jar = extract();
        if (jar == null) {
            return null;
        }
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(jar.toFile()));
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] bootstrap bridge rejected: " + throwable);
            return null;
        }
        return bridgeOrNull();
    }

    private static Path extract() {
        try (InputStream stream = BridgeLink.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return null;
            }
            Path path = Files.createTempDirectory("frmc-substratum").resolve("substratum-bridge.jar");
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
            return path;
        } catch (IOException exception) {
            System.err.println("[FRMC/substratum] cannot extract bridge: " + exception);
            return null;
        }
    }

    private static Class<?> bridgeOrNull() {
        try {
            return Class.forName(BRIDGE, false, null);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
