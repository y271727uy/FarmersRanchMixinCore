package com.y271727uy.frmc.mixinasm;

import org.objectweb.asm.tree.ClassNode;

@FunctionalInterface
public interface ClassEditor {
    /**
     * @return true when {@code node} was modified
     */
    boolean edit(ClassNode node);
}
