package com.y271727uy.frmc.mixinagent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;

final class InstrumentationAccess {
    private static volatile Instrumentation local;
    private static volatile boolean attached;

    private InstrumentationAccess() {
    }

    static void bind(Instrumentation instrumentation, SubstratumRegistry registry) {
        if (instrumentation == null) {
            return;
        }
        local = instrumentation;
        BridgeLink.install(instrumentation);
        if (!BridgeLink.claimTransformer()) {
            return;
        }
        instrumentation.addTransformer(new InstrumentationHook(registry), true);
    }

    static Instrumentation current() {
        Instrumentation bridged = BridgeLink.instrumentation();
        return bridged != null ? bridged : local;
    }

    static boolean available() {
        return current() != null;
    }

    static void tryAttach() {
        if (available() || attached) {
            return;
        }
        attached = true;
        try {
            Module self = InstrumentationAccess.class.getModule();
            java.lang.ModuleLayer.boot().findModule("jdk.attach").ifPresent(self::addReads);
            Class<?> virtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine");
            Path agent = agentJar();
            if (agent == null) {
                return;
            }
            String pid = Long.toString(ProcessHandle.current().pid());
            Object vm = virtualMachine.getMethod("attach", String.class).invoke(null, pid);
            try {
                virtualMachine.getMethod("loadAgent", String.class).invoke(vm, agent.toAbsolutePath().toString());
            } finally {
                virtualMachine.getMethod("detach").invoke(vm);
            }
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] self-attach unavailable: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    static boolean retransform(String className) {
        Instrumentation instrumentation = current();
        if (instrumentation == null || className == null) {
            return false;
        }
        String dotted = com.y271727uy.frmc.mixinasm.Bytecodes.dotted(className);
        try {
            for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                if (type != null && dotted.equals(type.getName()) && instrumentation.isModifiableClass(type)) {
                    instrumentation.retransformClasses(type);
                    return true;
                }
            }
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] retransform refused " + dotted + ": " + throwable);
        }
        return false;
    }

    private static Path agentJar() {
        try {
            var source = InstrumentationAccess.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            Path path = Path.of(source.getLocation().toURI());
            if (!java.nio.file.Files.isRegularFile(path)) {
                return null;
            }
            try (JarFile jar = new JarFile(path.toFile())) {
                if (jar.getJarEntry("com/y271727uy/frmc/mixinagent/SubstratumAgent.class") == null) {
                    return null;
                }
            }
            return path;
        } catch (Exception exception) {
            return null;
        }
    }
}
