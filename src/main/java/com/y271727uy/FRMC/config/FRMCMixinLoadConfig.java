package com.y271727uy.FRMC.config;

import com.y271727uy.frmc.mixinfabricextras.MixinFabricExtras;
import java.util.List;
import java.util.function.Predicate;

import net.minecraftforge.fml.loading.FMLLoader;

/** Mixin 加载配置：可选模组集成与兼容性层的加载条件统一在此定义。 */
public final class FRMCMixinLoadConfig {
    private static final String MIXIN_PACKAGE = "com.y271727uy.FRMC.mixin.";
    private static final List<String> BASE_MIXIN_PACKAGE_PREFIXES = List.of("minecraft.blockchecking.timing.");
    /**
     * 蚯蚓掉落只存在于 MCreator 包 {@code net.mcreator.vietnamdelight.WormDropHandler}。
     * 1.1.3 仍是同一个 modid，但已经删掉这个类，不能按 modid 硬套。
     */
    private static final String VIETNAM_WORM_MIXIN = "vietnam_delight.WormDropHandlerMixin";
    private static final String VIETNAM_WORM_TARGET = "net.mcreator.vietnamdelight.WormDropHandler";
    private static final String FASTCHUNKGEN_DFU_MIXIN = "fastchunkgen.TypeMixin";
    private static final String FASTCHUNKGEN_DFU_TARGET = "com.misanthropy.fastchunkgen.base.common.util.CFUtil";

    private static final List<OptionalMixinIntegration> OPTIONAL_MIXIN_INTEGRATIONS = List.of(
        new OptionalMixinIntegration("constancy.", "constancy", "modernfix"),
        new OptionalMixinIntegration("minecraft.recipe.deduplicator.farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("alexsmobs.", "alexsmobs"),
        new OptionalMixinIntegration("aquaculture.", "aquaculture"),
        new OptionalMixinIntegration("acceleratedrendering.", "acceleratedrendering"),
        new OptionalMixinIntegration("betterfoliage.", "betterfoliage"),
        new OptionalMixinIntegration("braziliandelight.", "braziliandelight"),
        new OptionalMixinIntegration("blueprint.", "blueprint", "neapolitan", "regions_unexplored"),
        new OptionalMixinIntegration("chloride.", "chloride"),
        new OptionalMixinIntegration("citadel.", "citadel"),
        new OptionalMixinIntegration("extradelight.", "extradelight"),
        new OptionalMixinIntegration("eggdelight.", "eggdelight"),
        new OptionalMixinIntegration("fastchunkgen.", "fastchunkgen"),
        new OptionalMixinIntegration("farm_and_charm.", "farm_and_charm"),
        new OptionalMixinIntegration("farmersdelight.", "farmersdelight"),
        new OptionalMixinIntegration("ftbquests.", "ftbquests"),
        new OptionalMixinIntegration("foragersinsight.", "foragersinsight"),
        new OptionalMixinIntegration("forestry.", "forestry"),
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
        new OptionalMixinIntegration("nethers_exoticism.", "nethers_exoticism"),
        new OptionalMixinIntegration("oceanic_delight.", "oceanic_delight"),
        new OptionalMixinIntegration("overweight_farming.", "overweight_farming"),
        new OptionalMixinIntegration("ruok.particlerain.", "ruokmod", "particlerain"),
        new OptionalMixinIntegration("stardew_valley_food.", "sdvf"),
        new OptionalMixinIntegration("starlight.", "starlight"),
        new OptionalMixinIntegration("guideme.", "guideme", "starlight"),
        new OptionalMixinIntegration("terrablender.", "terrablender"),
        new OptionalMixinIntegration("sereneseasons.", "sereneseasons"),
        new OptionalMixinIntegration("sinytra_connector.", "connectormod"),
        new OptionalMixinIntegration("thirst.", "thirst"),
        new OptionalMixinIntegration("untamed_wilds.", "untamedwilds"),
        new OptionalMixinIntegration("unusualfishmod.", "unusualfishmod"),
        new OptionalMixinIntegration("vietnam_delight.", "vietnam_delight"),
        new OptionalMixinIntegration("vinery.", "vinery"),
        new OptionalMixinIntegration("xaero.minimap.", "xaerominimap"),
        new OptionalMixinIntegration("xaero.worldmap.", "xaeroworldmap"),
        new OptionalMixinIntegration("youkaishomecoming.vinery.", "youkaishomecoming", "vinery"),
        new OptionalMixinIntegration("youkaishomecoming.", "youkaishomecoming")
    );

    private FRMCMixinLoadConfig() {
    }

    private static void initializeMixinFabricExtras() {
        try {
            MixinFabricExtras.init();
        } catch (Throwable ignored) {
            // MixinFabricExtras is optional and may be queried before Mixin is active.
        }
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
        initializeMixinFabricExtras();
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
        if (FASTCHUNKGEN_DFU_MIXIN.equals(relativeMixinName)) {
            return isClassPresent(FASTCHUNKGEN_DFU_TARGET);
        }
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
        if (VIETNAM_WORM_MIXIN.equals(relativeMixinName) && !isClassPresent(VIETNAM_WORM_TARGET)) {
            return false;
        }
        return true;
    }

    private static boolean isClassPresent(String binaryName) {
        String resource = binaryName.replace('.', '/') + ".class";
        try {
            ClassLoader loader = FRMCMixinLoadConfig.class.getClassLoader();
            return loader != null && loader.getResource(resource) != null;
        } catch (Throwable ignored) {
            return false;
        }
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
