package com.y271727uy.frmc.mixinasm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

public final class Bytecodes {
    public static final String MARKER = "Lcom/y271727uy/frmc/mixinasm/Applied;";

    private Bytecodes() {
    }

    public static String dotted(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replace('/', '.');
    }

    public static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }

    public static boolean applied(ClassNode node) {
        if (node == null || node.invisibleAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : node.invisibleAnnotations) {
            if (MARKER.equals(annotation.desc)) {
                return true;
            }
        }
        return false;
    }

    public static boolean applied(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        try {
            return applied(read(bytes));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static void mark(ClassNode node) {
        if (node == null || applied(node)) {
            return;
        }
        if (node.invisibleAnnotations == null) {
            node.invisibleAnnotations = new java.util.ArrayList<>();
        }
        node.invisibleAnnotations.add(new AnnotationNode(MARKER));
    }

    public static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String left, String right) {
                return "java/lang/Object";
            }
        };
        node.accept(writer);
        return writer.toByteArray();
    }

    public static byte[] edit(byte[] bytes, ClassEditor editor) {
        if (bytes == null || editor == null || applied(bytes)) {
            return null;
        }
        ClassNode node = read(bytes);
        if (!editor.edit(node)) {
            return null;
        }
        mark(node);
        return write(node);
    }

    public static void copyInto(ClassNode from, ClassNode into) {
        into.version = from.version;
        into.access = from.access;
        into.name = from.name;
        into.signature = from.signature;
        into.superName = from.superName;
        into.interfaces = from.interfaces;
        into.sourceFile = from.sourceFile;
        into.sourceDebug = from.sourceDebug;
        into.module = from.module;
        into.outerClass = from.outerClass;
        into.outerMethod = from.outerMethod;
        into.outerMethodDesc = from.outerMethodDesc;
        into.visibleAnnotations = from.visibleAnnotations;
        into.invisibleAnnotations = from.invisibleAnnotations;
        into.visibleTypeAnnotations = from.visibleTypeAnnotations;
        into.invisibleTypeAnnotations = from.invisibleTypeAnnotations;
        into.attrs = from.attrs;
        into.innerClasses = from.innerClasses;
        into.nestHostClass = from.nestHostClass;
        into.nestMembers = from.nestMembers;
        into.permittedSubclasses = from.permittedSubclasses;
        into.recordComponents = from.recordComponents;
        into.fields = from.fields;
        into.methods = from.methods;
    }
}
