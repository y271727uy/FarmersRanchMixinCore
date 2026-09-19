package com.y271727uy.FRMC.config;

import java.util.List;
import java.util.function.Predicate;

import net.minecraftforge.fml.loading.FMLLoader;

/** Mixin 加载配置：可选模组集成与兼容性层的加载条件统一在此定义。 */
public final class FRMCMixinLoadConfig {
    private static final String MIXIN_PACKAGE = "com.y271727uy.FRMC.mixin.";
    private static final List<String> BASE_MIXIN_PACKAGE_PREFIXES = List.of("minecraft.blockchecking.timing.");

    private static final List<OptionalMixinIntegration> OPTIONAL_MIXIN_INTEGRATIONS = List.of(
        new OptionalMixinIntegration("minecraft.recipe.deduplicator.farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("alexsmobs.", "alexsmobs"),
        new OptionalMixinIntegration("aquaculture.", "aquaculture"),
        new OptionalMixinIntegration("betterfoliage.", "betterfoliage"),
        new OptionalMixinIntegration("braziliandelight.", "braziliandelight"),
        new OptionalMixinIntegration("chloride.", "chloride"),
        new OptionalMixinIntegration("citadel.", "citadel"),
        new OptionalMixinIntegration("extradelight.", "extradelight"),
        new OptionalMixinIntegration("farm_and_charm.", "farm_and_charm"),
        new OptionalMixinIntegration("farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("fruits_delight.", "fruitsdelight"),
        new OptionalMixinIntegration("geckolib.", "geckolib"),
        new OptionalMixinIntegration("kaleidoscopetavern.vinery.", "kaleidoscope_tavern", "vinery"),
        new OptionalMixinIntegration("kaleidoscopetavern.", "kaleidoscope_tavern"),
        new OptionalMixinIntegration("kubejs.", "kubejs"),
        new OptionalMixinIntegration("manors_bounty.", "manors_bounty"),
        new OptionalMixinIntegration("manors_bounty_machine.", "manors_bounty_machine"),
        new OptionalMixinIntegration("modernui.", "modernui"),
        new OptionalMixinIntegration("mooncake_delight.", "mooncake_delight"),
        new OptionalMixinIntegration("mystiasizakaya.", "mystias_izakaya"),
        new OptionalMixinIntegration("nethervinery.", "nethervinery"),
        new OptionalMixinIntegration("stardew_valley_food.", "sdvf"),
        new OptionalMixinIntegration("starlight.", "starlight"),
        new OptionalMixinIntegration("sereneseasons.", "sereneseasons"),
        new OptionalMixinIntegration("thirst.", "thirst"),
        new OptionalMixinIntegration("untamed_wilds.", "untamedwilds"),
        new OptionalMixinIntegration("unusualfishmod.", "unusualfishmod"),
        new OptionalMixinIntegration("vinery.", "vinery"),
        new OptionalMixinIntegration("xaero.minimap.", "xaerominimap"),
        new OptionalMixinIntegration("xaero.worldmap.", "xaeroworldmap"),
        new OptionalMixinIntegration("youkaishomecoming.vinery.", "youkaishomecoming", "vinery"),
        new OptionalMixinIntegration("youkaishomecoming.", "youkaishomecoming")
    );

    private FRMCMixinLoadConfig() {
    }

    /** Runtime availability check for the optional Starlight/Create compatibility layer. */
    public static boolean isEnabled() {
        return isModLoaded("starlight") && isModLoaded("create");
    }

    /** Whether an optional integration mixin may load, based on the runtime mod list. */
    public static boolean isIntegrationEnabled(String mixinClassName) {
        return isIntegrationEnabled(mixinClassName, FRMCMixinLoadConfig::isModLoaded);
    }

    public static boolean isIntegrationEnabled(String mixinClassName, Predicate<String> modLoaded) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return true;
        }

        String relativeMixinName = mixinClassName.substring(MIXIN_PACKAGE.length());
        for (String baseMixinPrefix : BASE_MIXIN_PACKAGE_PREFIXES) {
            if (relativeMixinName.startsWith(baseMixinPrefix)) {
                return true;
            }
        }

        OptionalMixinIntegration match = null;
        for (OptionalMixinIntegration integration : OPTIONAL_MIXIN_INTEGRATIONS) {
            if (relativeMixinName.startsWith(integration.mixinPackagePrefix())
                    && (match == null || integration.mixinPackagePrefix().length() > match.mixinPackagePrefix().length())) {
                match = integration;
            }
        }
        if (match == null) {
            return true;
        }
        for (String requiredModId : match.modIds()) {
            if (!modLoaded.test(requiredModId)) {
                return false;
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

    private record OptionalMixinIntegration(String mixinPackagePrefix, String... modIds) {
    }
}
