package com.y271727uy.FRMC;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.compat.MooncakeDelightCroptopiaCompat;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// Basic mod entry point.
@Mod(FRMCMod.MODID)
@SuppressWarnings("removal")
public class FRMCMod {
    public static final String MODID = "frmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FRMCMod() {
        var modEventBus = MooncakeDelightCroptopiaCompat.getModEventBus();
        MooncakeDelightCroptopiaCompat.registerListeners(modEventBus);

        if (Config.recipeSearchEnabled) {
            LOGGER.info("Recipe search optimization enabled - using decision tree accelerated RecipeManagement");
        }

        if (Config.openglDiagnosticsEnabled) {
            LOGGER.info("OpenGL diagnostics enabled - logging frame stalls over {} ms and printing one-time context details", Config.openglStutterThresholdMs);
        }

        LOGGER.info("FRMC mod initialized");
    }
}
