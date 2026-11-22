package com.y271727uy.FRMC.integration.brewinandchewin;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID)
public final class KegWorldInteractionHandler {
    private KegWorldInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void allowSneakingUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && event.getUseBlock() != Event.Result.DENY
                && event.getLevel().getBlockEntity(event.getPos()) instanceof KegWorldPouring keg
                && keg.frmc$canTakeFromKeg(event.getItemStack())) {
            // Forge normally skips block use when sneaking with a held item.
            event.setUseBlock(Event.Result.ALLOW);
        }
    }
}
