package com.y271727uy.FRMC.mixin.blacklist;

import com.y271727uy.FRMC.config.FRMCMixinLoadConfig;
import com.y271727uy.FRMC.mixin.blacklist.application.FRMCMixinBlacklistApplication;
import com.y271727uy.FRMC.mixin.mixinsquared.MixinsquaredInitializer;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/*
 * 黑名单专用类
 * AI&PR
 * 加载条件（可选模组是否安装等）一律在 config 下的 FRMCMixinLoadConfig 中定义，
 * 本类只负责委托，禁止在此直接编写加载逻辑。
 */

public final class FRMCMixinConfigPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // Mixin wraps exceptions thrown here as a misleading "invalid resource" error.
        // Keep optional integrations isolated so the base config can still load.
        try {
            FRMCMixinBlacklistApplication.initialize();
        } catch (Throwable exception) {
            System.err.println("[FRMC] Failed to initialize mixin blacklist: " + exception);
        }
        try {
            MixinsquaredInitializer.initialize();
        } catch (Throwable exception) {
            System.err.println("[FRMC] Failed to initialize MixinSquared integration: " + exception);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!FRMCMixinLoadConfig.isIntegrationEnabled(mixinClassName)) {
            return false;
        }
        if (mixinClassName.startsWith("com.y271727uy.FRMC.mixin.starlight.")) {
            return FRMCMixinLoadConfig.isEnabled()
                && FRMCMixinBlacklistApplication.shouldApplyMixin(mixinClassName);
        }
        return FRMCMixinBlacklistApplication.shouldApplyMixin(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
