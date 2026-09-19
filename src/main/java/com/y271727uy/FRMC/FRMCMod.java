package com.y271727uy.FRMC;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.client.modelgap.ModelGapFixConfig;
import com.y271727uy.FRMC.client.FRMCClientSetup;
import com.y271727uy.FRMC.integration.manors_bounty.ManorsBountyIntegration;
import com.y271727uy.FRMC.integration.mooncakedelight.MooncakeDelightCroptopiaIntegration;
import com.y271727uy.FRMC.integration.vinery.VineryGrapejuiceTooltip;
import com.y271727uy.FRMC.capability.displaywine.DisplayWinePlacementHandler;
import com.y271727uy.FRMC.integration.farm_and_charm.FeedingTroughInteractionHandler;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.capability.entity.manager.entityactivity.EntityActivityManager;
import com.y271727uy.FRMC.config.EntityActivityConfig;
import com.y271727uy.FRMC.config.MobSpawnRateConfig;
import com.y271727uy.FRMC.config.SmoothBootConfig;
import com.y271727uy.FRMC.network.EntityActivityNetwork;
import com.y271727uy.FRMC.config.OnlineMusicConfig;
import com.y271727uy.FRMC.config.FRMCConfigPaths;
import com.y271727uy.FRMC.config.BlockCheckingConfig;
import com.y271727uy.FRMC.capability.guidance.AiGuidanceLogInjector;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingCommands;
import com.y271727uy.FRMC.capability.blockchecking.server.BlockCheckingRuntime;
import com.y271727uy.FRMC.network.BlockCheckingNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
// import com.y271727uy.FRMC.prefab.PrefabStructureCommands;
// import com.y271727uy.FRMC.prefab.PrefabBlueprintInteractionHandler;
// import com.y271727uy.FRMC.prefab.PrefabStructureReloadListener;
// import com.y271727uy.FRMC.prefab.PrefabStructureNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

// Basic mod entry point.
@Mod(FRMCMod.MODID)
@SuppressWarnings("removal")
public class FRMCMod {
    public static final String MODID = "frmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FRMCMod() {
        FRMCConfigPaths.ensureDirectory();
        AiGuidanceLogInjector.injectOnce();
        FRMCConfigPaths.ensureMusicListFile();
        FRMCConfigPaths.migrateLegacy("frmc-online-music.toml");
        FRMCConfigPaths.migrateLegacy("frmc-model-gap-fix.toml");
        FRMCConfigPaths.migrateLegacy("frmc-entity-activity.properties");
        FRMCConfigPaths.migrateLegacy("frmc-mob-spawn-rate.properties");
        FRMCConfigPaths.migrateLegacy("frmc-recipe-search.properties");
        FRMCConfigPaths.migrateLegacy("frmc-blockchecking.properties");
        FRMCConfigPaths.migrateLegacy("frmc-mixin-blacklist.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OnlineMusicConfig.SPEC, "frmc/frmc-online-music.toml");
        SmoothBootConfig smoothBootConfig = SmoothBootConfig.load();
        System.setProperty("frmc.smoothboot.bootstrapThreads", Integer.toString(smoothBootConfig.threadCount.bootstrap));
        System.setProperty("frmc.smoothboot.mainThreads", Integer.toString(smoothBootConfig.threadCount.main));
        System.setProperty("frmc.smoothboot.gamePriority", Integer.toString(smoothBootConfig.threadPriority.game));
        System.setProperty("frmc.smoothboot.integratedServerPriority", Integer.toString(smoothBootConfig.threadPriority.integratedServer));
        System.setProperty("frmc.smoothboot.bootstrapPriority", Integer.toString(smoothBootConfig.threadPriority.bootstrap));
        System.setProperty("frmc.smoothboot.mainPriority", Integer.toString(smoothBootConfig.threadPriority.main));
        System.setProperty("frmc.smoothboot.ioPriority", Integer.toString(smoothBootConfig.threadPriority.io));
        System.setProperty("frmc.smoothboot.modLoading", Integer.toString(smoothBootConfig.threadPriority.modLoading));
        ModelGapFixConfig.register();

        var modEventBus = MooncakeDelightCroptopiaIntegration.getModEventBus();
        MooncakeDelightCroptopiaIntegration.registerListeners(modEventBus);
        ManorsBountyIntegration.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(VineryGrapejuiceTooltip.class);
        MinecraftForge.EVENT_BUS.register(DisplayWinePlacementHandler.class);
        if (ModList.get().isLoaded("farm_and_charm")) {
            MinecraftForge.EVENT_BUS.register(FeedingTroughInteractionHandler.class);
        }
        EntityActivityNetwork.register();
        MinecraftForge.EVENT_BUS.register(EntityActivityNetwork.class);
        BlockCheckingNetwork.register();
        MinecraftForge.EVENT_BUS.register(BlockCheckingNetwork.class);
        EntityActivityConfig.load();
        MobSpawnRateConfig.load();
        BlockCheckingConfig.load();
        MinecraftForge.EVENT_BUS.register(EntityActivityManager.class);
        MinecraftForge.EVENT_BUS.register(BlockCheckingRuntime.class);
        MinecraftForge.EVENT_BUS.register(BlockCheckingCommands.class);
        // MinecraftForge.EVENT_BUS.addListener(PrefabStructureReloadListener::onAddReloadListeners);
        // MinecraftForge.EVENT_BUS.addListener(PrefabStructureCommands::onRegisterCommands);
        // MinecraftForge.EVENT_BUS.addListener(PrefabStructureNetwork::onDatapackSync);
        // MinecraftForge.EVENT_BUS.addListener(PrefabBlueprintInteractionHandler::onRightClickBlock);
        // PrefabStructureNetwork.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FRMCClientSetup.register(modEventBus));

        if (Config.recipeSearchEnabled) {
            LOGGER.info("Recipe search optimization enabled - using decision tree accelerated RecipeManagement");
        }

        if (Config.openglFrameProbeEnabled) {
            LOGGER.info("OpenGL frame probe enabled - logging frame stalls over {} ms and printing one-time context details", Config.openglStutterThresholdMs);
        }

        LOGGER.info("FRMC mod initialized");
    }
}
