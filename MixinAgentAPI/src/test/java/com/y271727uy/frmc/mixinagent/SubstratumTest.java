package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import com.y271727uy.frmc.mixinasm.MethodEdits;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubstratumTest {
    @AfterEach
    void reset() {
        Substratum.resetForTests();
    }

    @Test
    void rewritesForgeLoaderMethodThroughLaunchPluginAndAgentBytes() {
        Substratum.edit("fmlloader.production", "net.minecraftforge.fml.loading.FMLLoader",
                node -> MethodEdits.setBooleanConstant(node, "isProduction", "()Z", true));

        ClassNode node = loaderClass();
        EnumSet<ILaunchPluginService.Phase> phases = Substratum.plugin().handlesClass(
                Type.getObjectType(node.name), false, "classloading");
        assertEquals(EnumSet.of(ILaunchPluginService.Phase.BEFORE), phases);

        int flags = Substratum.plugin().processClassWithFlags(
                ILaunchPluginService.Phase.BEFORE, node, Type.getObjectType(node.name), "classloading");
        assertEquals(ILaunchPluginService.ComputeFlags.COMPUTE_FRAMES, flags);
        assertEquals(Opcodes.ICONST_1, MethodEdits.find(node, "isProduction", "()Z").instructions.getFirst().getOpcode());

        int second = Substratum.plugin().processClassWithFlags(
                ILaunchPluginService.Phase.BEFORE, node, Type.getObjectType(node.name), "classloading");
        assertEquals(ILaunchPluginService.ComputeFlags.NO_REWRITE, second);

        byte[] rewritten = Substratum.rewrite("net/minecraftforge/fml/loading/FMLLoader", originalBytes());
        ClassNode fromAgent = Bytecodes.read(rewritten);
        assertEquals(Opcodes.ICONST_1, MethodEdits.find(fromAgent, "isProduction", "()Z").instructions.getFirst().getOpcode());
        assertTrue(Bytecodes.applied(fromAgent));
        assertNull(Substratum.rewrite("net/minecraftforge/fml/loading/FMLLoader", rewritten));
    }

    @Test
    void ignoresUnrelatedClassesAndFrameComputation() {
        Substratum.edit("modlauncher.only", "cpw.mods.modlauncher.Launcher", node -> true);
        ClassNode node = loaderClass();
        assertTrue(Substratum.plugin().handlesClass(Type.getObjectType("cpw/mods/modlauncher/Launcher"), false).contains(ILaunchPluginService.Phase.BEFORE));
        assertTrue(Substratum.plugin().handlesClass(Type.getObjectType(node.name), false, "computing_frames").isEmpty());
        assertFalse(Substratum.exactTargets().contains("net.minecraftforge.fml.loading.FMLLoader"));
        assertTrue(Substratum.exactTargets().contains("cpw.mods.modlauncher.Launcher"));
    }

    @Test
    void packageEditMatchesForgeAndFml() {
        Substratum.editPackage("forge.package", "net.minecraftforge.fml", node -> false);
        assertTrue(Substratum.plugin().handlesClass(
                Type.getObjectType("net/minecraftforge/fml/loading/FMLServiceProvider"), false).contains(ILaunchPluginService.Phase.BEFORE));
        assertTrue(Substratum.plugin().handlesClass(
                Type.getObjectType("net/minecraftforge/common/ForgeHooks"), false).isEmpty());
    }

    private static ClassNode loaderClass() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = "net/minecraftforge/fml/loading/FMLLoader";
        node.superName = "java/lang/Object";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "isProduction", "()Z", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxStack = 1;
        method.maxLocals = 0;
        node.methods.add(method);
        return node;
    }

    private static byte[] originalBytes() {
        return Bytecodes.write(loaderClass());
    }
}
