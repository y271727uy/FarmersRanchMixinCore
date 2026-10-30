package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import com.y271727uy.frmc.mixinasm.ClassEditor;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

final class SubstratumRegistry {
    private final CopyOnWriteArrayList<Binding> bindings = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, BiFunction<String, byte[], byte[]>> foreign = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> foreignOrder = new CopyOnWriteArrayList<>();

    void add(String id, String className, boolean prefix, ClassEditor editor) {
        if (id == null || className == null || className.isEmpty() || editor == null) {
            return;
        }
        for (Binding binding : bindings) {
            if (id.equals(binding.id())) {
                return;
            }
        }
        bindings.add(new Binding(id, className, prefix, editor));
    }

    void addForeign(String id, BiFunction<String, byte[], byte[]> transform) {
        if (id == null || transform == null) {
            return;
        }
        if (foreign.putIfAbsent(id, transform) == null) {
            foreignOrder.addIfAbsent(id);
        }
    }

    boolean matches(String className) {
        // This method runs from ModLauncher's class-loading callback. Do not call
        // Bytecodes here: resolving that helper for the first time can re-enter
        // this callback while the helper class is still being defined.
        String dotted = className == null ? "" : className.replace('/', '.');
        for (Binding binding : bindings) {
            if (binding.matches(dotted)) {
                return true;
            }
        }
        return !foreign.isEmpty();
    }

    boolean apply(ClassNode node) {
        if (node == null || node.name == null || Bytecodes.applied(node)) {
            return false;
        }
        String dotted = Bytecodes.dotted(node.name);
        boolean changed = false;
        for (Binding binding : bindings) {
            if (binding.matches(dotted)) {
                changed |= binding.editor().edit(node);
            }
        }
        return changed;
    }

    byte[] applyForeign(String className, byte[] bytes) {
        if (bytes == null || foreign.isEmpty() || Bytecodes.applied(bytes)) {
            return null;
        }
        byte[] current = bytes;
        boolean changed = false;
        for (String id : foreignOrder) {
            if (hasId(id)) {
                continue;
            }
            BiFunction<String, byte[], byte[]> transform = foreign.get(id);
            if (transform == null) {
                continue;
            }
            byte[] next = transform.apply(className, current);
            if (next != null && next != current) {
                current = next;
                changed = true;
            }
        }
        return changed ? current : null;
    }

    boolean hasId(String id) {
        for (Binding binding : bindings) {
            if (id.equals(binding.id())) {
                return true;
            }
        }
        return false;
    }

    Set<String> exactNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Binding binding : bindings) {
            if (!binding.prefix()) {
                names.add(binding.className());
            }
        }
        return names;
    }

    List<Binding> bindings() {
        return new ArrayList<>(bindings);
    }

    void clear() {
        bindings.clear();
        foreign.clear();
        foreignOrder.clear();
    }

    record Binding(String id, String className, boolean prefix, ClassEditor editor) {
        boolean matches(String dotted) {
            return prefix ? dotted.startsWith(className) : dotted.equals(className);
        }
    }
}
