package com.y271727uy.FRMC.client.modelgap;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModelGapFixClientEvents {
    private ModelGapFixClientEvents() {
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != ModelGapFixConfig.SPEC) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.getResourceManager() != null) {
            minecraft.reloadResourcePacks();
        }
    }
}
