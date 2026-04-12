package com.y271727uy.FRMC;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// Basic mod entry point.
@Mod(FRMCMod.MODID)
public class FRMCMod {
    public static final String MODID = "frmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FRMCMod() {
        LOGGER.info("FRMC mod initialized");
    }
}
