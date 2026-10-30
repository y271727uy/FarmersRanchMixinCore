package com.y271727uy.frmc.mixinasm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;

public final class MethodEdits {
    private MethodEdits() {
    }

    public static boolean rewrite(ClassNode node, String name, String descriptor, Consumer<MethodNode> rewriter) {
        MethodNode method = find(node, name, descriptor);
        if (method == null || rewriter == null) {
            return false;
        }
        rewriter.accept(method);
        return true;
    }

    public static boolean setBooleanConstant(ClassNode node, String name, String descriptor, boolean value) {
        return rewrite(node, name, descriptor, method -> {
            method.instructions.clear();
            if (method.tryCatchBlocks != null) {
                method.tryCatchBlocks.clear();
            }
            method.instructions.add(new InsnNode(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            method.maxStack = 1;
            method.maxLocals = Math.max(method.maxLocals, 0);
        });
    }

    public static MethodNode find(ClassNode node, String name, String descriptor) {
        if (node == null || node.methods == null) {
            return null;
        }
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && (descriptor == null || descriptor.equals(method.desc))) {
                return method;
            }
        }
        return null;
    }
}
