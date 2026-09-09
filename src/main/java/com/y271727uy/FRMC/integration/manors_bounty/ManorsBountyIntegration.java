package com.y271727uy.FRMC.integration.manors_bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.manors_bounty.init.ManorsBountyModItems;

public final class ManorsBountyIntegration {
    private static final String MANORS_BOUNTY = "manors_bounty";
    private static final ResourceLocation MAIN_TAB = new ResourceLocation(MANORS_BOUNTY, "manors_bounty");

    private ManorsBountyIntegration() {
    }

    public static void register(IEventBus modEventBus) {
        if (!ModList.get().isLoaded(MANORS_BOUNTY)) {
            return;
        }

        modEventBus.addListener(ManorsBountyIntegration::onBuildCreativeModeTabContents);
    }

    private static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();
        if (MAIN_TAB.equals(tabId)) {
            accept(event, ManorsBountyModItems.TURKEY_EGG);
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, RegistryObject<Item> item) {
        if (item.isPresent()) {
            event.accept(item.get());
        }
    }
}
