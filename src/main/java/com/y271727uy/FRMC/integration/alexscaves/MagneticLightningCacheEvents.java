package com.y271727uy.FRMC.integration.alexscaves;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, value = Dist.CLIENT)
public final class MagneticLightningCacheEvents {
    private MagneticLightningCacheEvents() {
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        var entity = event.getEntity();
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer instanceof MagneticLightningCache magnetic) {
            magnetic.frmc$getLightningRenderMap().remove(entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        for (var renderer : Minecraft.getInstance().getEntityRenderDispatcher().renderers.values()) {
            if (renderer instanceof MagneticLightningCache magnetic) {
                magnetic.frmc$getLightningRenderMap().clear();
            }
        }
    }
}
