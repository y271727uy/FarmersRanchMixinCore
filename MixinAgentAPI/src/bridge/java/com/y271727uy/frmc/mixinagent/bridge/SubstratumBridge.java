package com.y271727uy.frmc.mixinagent.bridge;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bootstrap-loader mailbox. The agent, the ModLauncher service, and the game
 * layer each have their own copy of the agent classes; this type is the one
 * copy they can all see.
 */
public final class SubstratumBridge {
    public interface BytesTransform {
        byte[] transform(String className, byte[] classBytes);
    }

    private static final ConcurrentHashMap<String, BytesTransform> TRANSFORMS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> ORDER = new CopyOnWriteArrayList<>();
    private static volatile Object instrumentation;
    private static volatile boolean transformerClaimed;

    private SubstratumBridge() {
    }

    public static void bind(Object value) {
        if (value != null) {
            instrumentation = value;
        }
    }

    public static Object instrumentation() {
        return instrumentation;
    }

    public static boolean claimTransformer() {
        if (transformerClaimed) {
            return false;
        }
        transformerClaimed = true;
        return true;
    }

    public static boolean add(String id, BytesTransform transform) {
        if (id == null || transform == null) {
            return false;
        }
        if (TRANSFORMS.putIfAbsent(id, transform) != null) {
            return false;
        }
        ORDER.addIfAbsent(id);
        return true;
    }

    public static byte[] apply(String className, byte[] classBytes) {
        if (className == null || classBytes == null) {
            return null;
        }
        byte[] current = classBytes;
        boolean changed = false;
        for (String id : ORDER) {
            BytesTransform transform = TRANSFORMS.get(id);
            if (transform == null) {
                continue;
            }
            byte[] next = transform.transform(className, current);
            if (next != null && next != current) {
                current = next;
                changed = true;
            }
        }
        return changed ? current : null;
    }
}
