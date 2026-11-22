package com.y271727uy.FRMC.coremod.mcreator;

import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时注入的 ModLauncher launch plugin，可以看到每一个被加载的类。
 *
 * 注册方式参考 ModernFix 的 CoreLaunchPluginService：在 mixin config plugin 的 onLoad 中
 * 反射拿到 Launcher.launchPlugins -> LaunchPluginHandler.plugins，复制成 LinkedHashMap 后把自己
 * 追加到末尾。这样普通 mod jar 就能注册，不需要单独的 service 层 jar。
 *
 * 当前阶段只做识别 + 报告（{@link McreatorScanner}），不改写任何字节码，始终返回 NO_REWRITE。
 *
 * 关闭开关：JVM 参数 -Dfrmc.mcreator.scan=false
 *
 * 注意：本类在类加载链路上被调用，禁止引用任何游戏类（net.minecraft / net.minecraftforge 运行时类），
 * 否则会引起递归类加载。
 */
public final class FRMCLaunchPlugin implements ILaunchPluginService {
    public static final String NAME = "frmc_mcreator";
    static final Logger LOGGER = LoggerFactory.getLogger("FRMC-LaunchPlugin");

    private static final EnumSet<Phase> GO = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NOGO = EnumSet.noneOf(Phase.class);
    /** ITransformerActivity.CLASSLOADING_REASON；frame 计算等其它原因不处理。 */
    private static final String CLASSLOADING_REASON = "classloading";
    private static final String MCREATOR_PREFIX = "net.mcreator.";

    private static volatile boolean installed;

    public static synchronized void install() {
        if (installed) {
            return;
        }
        if ("false".equalsIgnoreCase(System.getProperty("frmc.mcreator.scan"))) {
            LOGGER.info("MCreator scanner disabled by -Dfrmc.mcreator.scan=false");
            return;
        }
        try {
            // 预加载扫描路径上会用到的所有自有类，避免在 processClass 中触发递归加载
            McreatorScanner.warmup();

            Field launchPluginsField = Launcher.class.getDeclaredField("launchPlugins");
            launchPluginsField.setAccessible(true);
            Object handler = launchPluginsField.get(Launcher.INSTANCE);

            Field pluginsField = LaunchPluginHandler.class.getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ILaunchPluginService> current = (Map<String, ILaunchPluginService>) pluginsField.get(handler);

            Map<String, ILaunchPluginService> replacement = new LinkedHashMap<>(current);
            FRMCLaunchPlugin plugin = new FRMCLaunchPlugin();
            replacement.put(plugin.name(), plugin);
            pluginsField.set(handler, replacement);

            McreatorScanner.registerShutdownReport();
            installed = true;
            LOGGER.info("Installed MCreator scan launch plugin (report-only). Plugin order: {}", replacement.keySet());
        } catch (Throwable t) {
            LOGGER.error("Failed to install MCreator scan launch plugin; scanner disabled", t);
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return handlesClass(classType, isEmpty, CLASSLOADING_REASON);
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        // 每个类都会调用到这里，判断必须极廉价
        if (isEmpty || !CLASSLOADING_REASON.equals(reason)) {
            return NOGO;
        }
        return classType.getClassName().startsWith(MCREATOR_PREFIX) ? GO : NOGO;
    }

    @Override
    public int processClassWithFlags(Phase phase, ClassNode classNode, Type classType, String reason) {
        try {
            McreatorScanner.scan(classNode);
        } catch (Throwable t) {
            LOGGER.warn("MCreator scan failed for {}", classType.getClassName(), t);
        }
        return ComputeFlags.NO_REWRITE;
    }
}
