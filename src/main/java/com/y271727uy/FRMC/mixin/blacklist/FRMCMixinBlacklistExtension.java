package com.y271727uy.FRMC.mixin.blacklist;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.SortedSet;
import java.util.function.Predicate;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

public final class FRMCMixinBlacklistExtension implements IExtension, Predicate<IMixinInfo> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FRMCMixinBlacklistExtension INSTANCE = new FRMCMixinBlacklistExtension();
    private static final String FRMC_PACKAGE_PREFIX = "com.y271727uy.FRMC.";
    private static final Class<?> TARGET_CLASS_CONTEXT_CLASS;
    private static final Field MIXINS_FIELD;

    private static volatile boolean registered;

    private FRMCMixinBlacklistExtension() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        synchronized (FRMCMixinBlacklistExtension.class) {
            if (registered) {
                return;
            }

            IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
            Extensions extensions = (Extensions) transformer.getExtensions();
            extensions.add(INSTANCE);
            registered = true;
            LOGGER.info("Registered FRMC mixin blacklist extension");
        }
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
        if (!TARGET_CLASS_CONTEXT_CLASS.isInstance(context)) {
            return;
        }

        try {
            SortedSet<?> mixins = (SortedSet<?>) MIXINS_FIELD.get(context);
            mixins.removeIf(candidate -> candidate instanceof IMixinInfo mixinInfo && test(mixinInfo));
        } catch (IllegalAccessException exception) {
            throw new RuntimeException("Failed to filter mixins from target class context", exception);
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {
    }

    @Override
    public void export(MixinEnvironment environment, String name, boolean force, ClassNode classNode) {
    }

    @Override
    public boolean test(IMixinInfo mixinInfo) {
        String mixinClassName = mixinInfo.getClassName();
        if (mixinClassName.startsWith(FRMC_PACKAGE_PREFIX)) {
            return false;
        }

        if (!FRMCMixinBlacklistConfig.isBlacklisted(mixinClassName)) {
            return false;
        }

        LOGGER.warn("Blocked blacklisted mixin {} targeting {}", mixinClassName, mixinInfo.getTargetClasses());
        return true;
    }

    static {
        try {
            TARGET_CLASS_CONTEXT_CLASS = Class.forName("org.spongepowered.asm.mixin.transformer.TargetClassContext");
            MIXINS_FIELD = TARGET_CLASS_CONTEXT_CLASS.getDeclaredField("mixins");
            MIXINS_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}



