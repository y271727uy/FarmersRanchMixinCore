package com.y271727uy.FRMC.client.untamed_wilds;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class UntamedWildsCreativeTabRefresh {
    private static final int SETTLE_TICKS = 4;
    private static int ticksUntilRefresh = -1;

    private UntamedWildsCreativeTabRefresh() {
    }

    public static void markSpeciesSyncReceived() {
        ticksUntilRefresh = SETTLE_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ticksUntilRefresh < 0) {
            return;
        }

        if (--ticksUntilRefresh > 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, false, minecraft.level.registryAccess());
        }
    }
}
