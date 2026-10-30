package com.y271727uy.frmc.mixinasm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodesTest {
    @Test
    void editsOnceAndMarksTheClass() {
        byte[] rewritten = Bytecodes.edit(original(), node ->
                MethodEdits.setBooleanConstant(node, "isProduction", "()Z", true));
        ClassNode node = Bytecodes.read(rewritten);
        assertEquals(Opcodes.ICONST_1, MethodEdits.find(node, "isProduction", "()Z").instructions.getFirst().getOpcode());
        assertTrue(Bytecodes.applied(node));
        assertNull(Bytecodes.edit(rewritten, target -> true));
        assertFalse(Bytecodes.applied(new byte[]{0}));
    }

    private static byte[] original() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "net/minecraftforge/fml/loading/FMLLoader";
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "isProduction", "()Z", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
        return Bytecodes.write(node);
    }
}
