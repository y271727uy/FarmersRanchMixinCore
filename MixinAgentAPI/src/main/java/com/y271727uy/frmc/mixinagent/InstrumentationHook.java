package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import com.y271727uy.frmc.mixinasm.ClassEditor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

final class InstrumentationHook implements ClassFileTransformer {
    private final SubstratumRegistry registry;

    InstrumentationHook(SubstratumRegistry registry) {
        this.registry = registry;
    }

    @Override
    public byte[] transform(ClassLoader loader, String internalName, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
        return rewrite(registry, internalName, bytes);
    }

    static byte[] rewrite(SubstratumRegistry registry, String internalName, byte[] bytes) {
        if (internalName == null || bytes == null || bytes.length == 0) {
            return null;
        }
        String dotted = Bytecodes.dotted(internalName);
        if (dotted.startsWith("com.y271727uy.frmc.mixinagent.bridge.") || dotted.startsWith("java.") || dotted.startsWith("jdk.") || dotted.startsWith("sun.")) {
            return null;
        }
        if (Boolean.getBoolean("frmc.substratum.trace") && isForgeOrLauncher(dotted)) {
            System.err.println("[FRMC/substratum] seen " + dotted);
        }
        try {
            byte[] fromBridge = BridgeLink.apply(dotted, bytes);
            if (fromBridge != null) {
                return fromBridge;
            }
            if (registry == null || !registry.matches(dotted) || Bytecodes.applied(bytes)) {
                return null;
            }
            byte[] rewritten = Bytecodes.edit(bytes, new ClassNodeEdit(registry));
            if (rewritten == null) {
                byte[] foreign = registry.applyForeign(dotted, bytes);
                if (foreign == null) {
                    return null;
                }
                rewritten = foreign;
            } else {
                byte[] foreign = registry.applyForeign(dotted, rewritten);
                if (foreign != null) {
                    rewritten = foreign;
                }
            }
            return rewritten;
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] agent skipped " + dotted + ": " + throwable);
            return null;
        }
    }

    static boolean isForgeOrLauncher(String dotted) {
        return dotted.startsWith("net.minecraftforge.")
                || dotted.startsWith("cpw.mods.modlauncher.")
                || dotted.startsWith("cpw.mods.bootstraplauncher.");
    }

    private static final class ClassNodeEdit implements ClassEditor {
        private final SubstratumRegistry registry;

        private ClassNodeEdit(SubstratumRegistry registry) {
            this.registry = registry;
        }

        @Override
        public boolean edit(org.objectweb.asm.tree.ClassNode node) {
            return registry.apply(node);
        }
    }
}
