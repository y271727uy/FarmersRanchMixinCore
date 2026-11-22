package com.y271727uy.FRMC.client.jade;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.DrinkBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopetavern.item.BottleBlockItem;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernBarrelWine;
import com.y271727uy.FRMC.integration.kaleidoscopetavern.TavernMugs;
import com.y271727uy.FRMC.integration.vinery.GrapevinePressingBatch;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.satisfy.vinery.core.block.GrapevinePotBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.theme.IThemeHelper;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;

@WailaPlugin("frmc")
public final class FRMCJadePlugin implements IWailaPlugin {
    private static final String FEEDING_TROUGH_BLOCK_CLASS =
        "net.satisfy.farm_and_charm.core.block.FeedingTroughBlock";

    @Override
    public void register(IWailaCommonRegistration registration) {
        if (ModList.get().isLoaded("farmersrespite")) {
            registration.registerFluidStorage(KettleFluidJadeProvider.INSTANCE, KettleBlockEntity.class);
        }
        if (ModList.get().isLoaded("vinery")) {
            registration.registerBlockDataProvider(GrapevinePotJadeProvider.INSTANCE, GrapevinePressingBatch.class);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (ModList.get().isLoaded("farmersrespite")) {
            registration.registerFluidStorageClient(KettleFluidJadeProvider.INSTANCE);
        }
        if (ModList.get().isLoaded("vinery")) {
            registration.registerBlockComponent(GrapevinePotJadeProvider.INSTANCE, GrapevinePotBlock.class);
        }
        if (ModList.get().isLoaded("kaleidoscope_tavern")) {
            registration.addTooltipCollectedCallback(0, (tooltip, accessor) -> {
                if (accessor instanceof BlockAccessor block && !block.isFakeBlock()) {
                    showBarrelDrinkName(tooltip, block);
                }
            });
        }

        // Farm & Charm reduces SIZE when animals eat, which can leave its stored stack stale.
        registration.addTooltipCollectedCallback(0, (tooltip, accessor) -> {
            if (accessor instanceof BlockAccessor blockAccessor
                    && blockAccessor.getBlock().getClass().getName().equals(FEEDING_TROUGH_BLOCK_CLASS)) {
                tooltip.remove(Identifiers.UNIVERSAL_ITEM_STORAGE);
            }
        });
    }

    private static void showBarrelDrinkName(ITooltip tooltip, BlockAccessor accessor) {
        boolean glassOrMug = TavernBarrelWine.isCocktailDisplay(accessor.getBlockState())
            || accessor.getBlock() instanceof TavernMugs.FilledBlock;
        if ((!glassOrMug && !accessor.getBlockState().is(ModBlocks.WINE.get()))
                || !(accessor.getBlockEntity() instanceof DrinkBlockEntity bottle)) {
            return;
        }

        // The drink block gives the player the last occupied slot on pickup.
        var items = bottle.getItems();
        for (int slot = items.size() - 1; slot >= 0; slot--) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if ((glassOrMug || stack.is(TavernBarrelWine.OUTPUT)) && !tooltip.get(Identifiers.CORE_OBJECT_NAME).isEmpty()) {
                tooltip.remove(Identifiers.CORE_OBJECT_NAME);
                tooltip.add(0, IThemeHelper.get().title(stack.getHoverName()), Identifiers.CORE_OBJECT_NAME);
                int quality = BottleBlockItem.getBrewLevel(stack);
                if (quality > 0) {
                    tooltip.add(1, TavernBarrelWine.qualityLine(quality));
                }
            }
            return;
        }
    }
}
