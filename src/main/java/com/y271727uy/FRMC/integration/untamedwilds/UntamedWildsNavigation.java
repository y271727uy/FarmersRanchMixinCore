package com.y271727uy.FRMC.integration.untamedwilds;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.slf4j.Logger;
import untamedwilds.entity.ComplexMobTerrestrial;

/**
 * Every {@link ComplexMobTerrestrial} (bears, big cats, bison, boars, ...) navigates with a package-private
 * {@code AmphibiousPathNavigation} that searches with vanilla's amphibious node evaluator, so even a short
 * walk across flat grass explores vertical swim nodes. The class can only be matched by name.
 */
public final class UntamedWildsNavigation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TERRESTRIAL_NAVIGATION = "untamedwilds.entity.ComplexMobTerrestrial$AmphibiousPathNavigation";
    private static final Class<?> TERRESTRIAL_NAVIGATION_CLASS = resolve();

    private UntamedWildsNavigation() {
    }

    public static boolean isTerrestrialNavigation(PathNavigation navigation) {
        return TERRESTRIAL_NAVIGATION_CLASS != null && navigation.getClass() == TERRESTRIAL_NAVIGATION_CLASS;
    }

    private static Class<?> resolve() {
        try {
            return Class.forName(TERRESTRIAL_NAVIGATION, false, ComplexMobTerrestrial.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            LOGGER.warn("Untamed Wilds terrestrial navigation not found, short direct routes are disabled: {}",
                exception.toString());
            return null;
        }
    }
}
