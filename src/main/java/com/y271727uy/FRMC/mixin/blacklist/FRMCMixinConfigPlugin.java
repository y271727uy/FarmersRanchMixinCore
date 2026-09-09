package com.y271727uy.FRMC.mixin.blacklist;

import com.y271727uy.FRMC.config.FRMCMixinLoadConfig;
import com.y271727uy.FRMC.mixin.blacklist.application.FRMCMixinBlacklistApplication;
import com.y271727uy.FRMC.mixin.mixinsquared.MixinsquaredInitializer;

import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/*
 * 黑名单专用类
 */

public final class FRMCMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "com.y271727uy.FRMC.mixin.";
    private static final List<OptionalMixinIntegration> OPTIONAL_MIXIN_INTEGRATIONS = List.of(
        new OptionalMixinIntegration("minecraft.recipe.deduplicator.farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("alexsmobs.", "alexsmobs"),
        new OptionalMixinIntegration("aquaculture.", "aquaculture"),
        new OptionalMixinIntegration("chloride.", "chloride"),
        new OptionalMixinIntegration("citadel.", "citadel"),
        new OptionalMixinIntegration("extradelight.", "extradelight"),
        new OptionalMixinIntegration("farm_and_charm.", "farm_and_charm"),
        new OptionalMixinIntegration("farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("fruits_delight.", "fruitsdelight"),
        new OptionalMixinIntegration("geckoLib.", "geckolib"),
        new OptionalMixinIntegration("manors_bounty.", "manors_bounty"),
        new OptionalMixinIntegration("manors_bounty_machine.", "manors_bounty_machine"),
        new OptionalMixinIntegration("modernui.", "modernui"),
        new OptionalMixinIntegration("mooncake_delight.", "mooncake_delight"),
        new OptionalMixinIntegration("mystiasizakaya.", "mystias_izakaya"),
        new OptionalMixinIntegration("nethervinery.", "nethervinery"),
        new OptionalMixinIntegration("stardew_valley_food.", "sdvf"),
        new OptionalMixinIntegration("starlight.", "starlight"),
        new OptionalMixinIntegration("thirst.", "thirst"),
        new OptionalMixinIntegration("untamed_wilds.", "untamedwilds"),
        new OptionalMixinIntegration("unusualfishmod.", "unusualfishmod"),
        new OptionalMixinIntegration("vinery.", "vinery")
    );

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
        if (!isOptionalIntegrationAvailable(mixinClassName, FRMCMixinConfigPlugin::isModLoaded)) {
            return false;
        }
        if (mixinClassName.startsWith("com.y271727uy.FRMC.mixin.starlight.")) {
            return FRMCMixinLoadConfig.isEnabled()
                && FRMCMixinBlacklistApplication.shouldApplyMixin(mixinClassName);
        }
        return FRMCMixinBlacklistApplication.shouldApplyMixin(mixinClassName);
    }

    static boolean isOptionalIntegrationAvailable(String mixinClassName, Predicate<String> modLoaded) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return true;
        }

        String relativeMixinName = mixinClassName.substring(MIXIN_PACKAGE.length());
        for (OptionalMixinIntegration integration : OPTIONAL_MIXIN_INTEGRATIONS) {
            if (relativeMixinName.startsWith(integration.mixinPackagePrefix())) {
                return modLoaded.test(integration.modId());
            }
        }

        return true;
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FMLLoader.getLoadingModList().getModFileById(modId) != null;
        } catch (Throwable ignored) {
            // During early loading, a failed availability check must disable optional mixins.
            return false;
        }
    }

    private record OptionalMixinIntegration(String mixinPackagePrefix, String modId) {
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
