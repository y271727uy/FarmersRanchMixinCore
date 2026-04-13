package com.y271727uy.FRMC;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.compat.MooncakeDelightCroptopiaCompat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Basic mod entry point.
@Mod(FRMCMod.MODID)
public class FRMCMod {
    public static final String MODID = "frmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FRMCMod() {
        MooncakeDelightCroptopiaCompat.registerListeners(FMLJavaModLoadingContext.get().getModEventBus());
        LOGGER.info("FRMC mod initialized");
    }
}
