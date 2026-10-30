package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.function.BiFunction;

public final class SubstratumLaunchPlugin implements ILaunchPluginService {
    private final SubstratumRegistry registry;

    SubstratumLaunchPlugin(SubstratumRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return LaunchPluginIntrusion.PLUGIN_NAME;
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        if (isEmpty || classType == null || "computing_frames".equals(reason)) {
            return EnumSet.noneOf(Phase.class);
        }
        if (!registry.matches(classType.getClassName())) {
            return EnumSet.noneOf(Phase.class);
        }
        return EnumSet.of(Phase.BEFORE);
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return handlesClass(classType, isEmpty, "classloading");
    }

    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        if (phase != Phase.BEFORE || classNode == null || "computing_frames".equals(reason)) {
            return ComputeFlags.NO_REWRITE;
        }
        try {
            if (Bytecodes.applied(classNode)) {
                return ComputeFlags.NO_REWRITE;
            }
            boolean changed = registry.apply(classNode);
            byte[] foreign = registry.applyForeign(classType == null ? Bytecodes.dotted(classNode.name) : classType.getClassName(), Bytecodes.write(classNode));
            if (foreign != null) {
                Bytecodes.copyInto(Bytecodes.read(foreign), classNode);
                changed = true;
            }
            if (!changed) {
                return ComputeFlags.NO_REWRITE;
            }
            Bytecodes.mark(classNode);
            return ComputeFlags.COMPUTE_FRAMES;
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] launch plugin skipped " + classNode.name + ": " + throwable);
            return ComputeFlags.NO_REWRITE;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtension() {
        return (T) (java.util.function.BiConsumer<String, BiFunction<String, byte[], byte[]>>) registry::addForeign;
    }
}
