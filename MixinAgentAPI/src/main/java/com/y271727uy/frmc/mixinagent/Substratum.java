package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import com.y271727uy.frmc.mixinasm.ClassEditor;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Loading path under Mixin and ModLauncher's normal transformers.
 *
 * <p>Game-layer Forge classes are rewritten by an {@code ILaunchPluginService} forced into
 * ModLauncher's plugin map. {@code net.minecraftforge.fml.loading} and {@code cpw.mods.modlauncher}
 * are already defined by then, so those need {@code -javaagent:MixinAgentAPI.jar}.
 * Already-loaded classes can still have method bodies replaced through retransform.
 */
public final class Substratum {
    private static final SubstratumRegistry REGISTRY = new SubstratumRegistry();
    private static final SubstratumLaunchPlugin PLUGIN = new SubstratumLaunchPlugin(REGISTRY);
    private static volatile boolean installed;
    private static volatile State state = new State(false, false, false, 0);

    private Substratum() {
    }

    public record State(boolean launchPlugin, boolean instrumentation, boolean bridge, int openedModules) {
    }

    public static void install() {
        install(true);
    }

    public static synchronized void install(boolean allowAttach) {
        if (installed) {
            return;
        }
        installed = true;
        try {
            if (allowAttach) {
                InstrumentationAccess.tryAttach();
            }
            ForgeFmlHooks.registerBuiltIn();
            boolean plugin = LaunchPluginIntrusion.install(PLUGIN);
            int opened = ForgeFmlHooks.openLoadedPlatformModules();
            state = new State(plugin, InstrumentationAccess.available(), BridgeLink.available(), opened);
            System.err.println("[FRMC/substratum] plugin=" + state.launchPlugin
                    + " agent=" + state.instrumentation
                    + " bridge=" + state.bridge
                    + " opened=" + state.openedModules);
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] install failed: " + throwable);
        }
    }

    public static State state() {
        return state;
    }

    /** Exact class, dotted or slashed. Editors should be idempotent: the agent and the launch plugin can both see a class. */
    public static void edit(String className, ClassEditor editor) {
        edit(className + "#" + System.identityHashCode(editor), className, editor);
    }

    public static void edit(String id, String className, ClassEditor editor) {
        edit(id, className, false, editor);
    }

    public static void editPackage(String id, String packagePrefix, ClassEditor editor) {
        String prefix = Bytecodes.dotted(packagePrefix);
        if (!prefix.endsWith(".")) {
            prefix = prefix + ".";
        }
        edit(id, prefix, true, editor);
    }

    public static boolean retransform(String className) {
        return InstrumentationAccess.retransform(className);
    }

    public static byte[] rewrite(String internalName, byte[] bytes) {
        return InstrumentationHook.rewrite(REGISTRY, internalName, bytes);
    }

    public static SubstratumLaunchPlugin plugin() {
        return PLUGIN;
    }

    static SubstratumRegistry registry() {
        return REGISTRY;
    }

    public static ITransformer<ClassNode> transformer() {
        return new SubstratumTransformer(REGISTRY);
    }

    public static boolean installInto(Object launchPluginHandler) {
        return LaunchPluginIntrusion.insert(launchPluginHandler, PLUGIN);
    }

    public static Set<String> exactTargets() {
        return REGISTRY.exactNames();
    }

    public static synchronized void resetForTests() {
        REGISTRY.clear();
        installed = false;
        state = new State(false, false, false, 0);
    }

    private static void edit(String id, String className, boolean prefix, ClassEditor editor) {
        String normalized = Bytecodes.dotted(className);
        if (id == null || normalized.isEmpty() || editor == null) {
            return;
        }
        REGISTRY.add(id, normalized, prefix, editor);
        BridgeLink.publish(id, normalized, prefix, editor);
        publishForeign(id, normalized, prefix, editor);
        if (!prefix && InstrumentationAccess.available()) {
            retransform(normalized);
        }
    }

    @SuppressWarnings("unchecked")
    private static void publishForeign(String id, String className, boolean prefix, ClassEditor editor) {
        Object installedPlugin = LaunchPluginIntrusion.installed();
        if (!(installedPlugin instanceof ILaunchPluginService service) || installedPlugin == PLUGIN) {
            return;
        }
        Object extension = service.getExtension();
        if (!(extension instanceof BiConsumer<?, ?> consumer)) {
            return;
        }
        BiFunction<String, byte[], byte[]> transform = (name, bytes) -> {
            String dotted = Bytecodes.dotted(name);
            boolean hit = prefix ? dotted.startsWith(className) : dotted.equals(className);
            return hit ? Bytecodes.edit(bytes, editor) : null;
        };
        ((BiConsumer<String, BiFunction<String, byte[], byte[]>>) consumer).accept(id, transform);
    }
}
