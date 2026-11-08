package com.y271727uy.FRMC.integration.manors_bounty;

import net.mcreator.manors_bounty.block.HoundstoothInfiniteCarpetBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "frmc", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HoundstoothCarpetModels {
    private static final ResourceLocation CARPET = new ResourceLocation("manors_bounty", "houndstooth_infinite_carpet");
    private static final ResourceLocation[] VARIANTS = new ResourceLocation[16];
    private static volatile boolean baked;

    static {
        for (int variant = 0; variant < VARIANTS.length; variant++) {
            VARIANTS[variant] = new ResourceLocation("frmc", "block/houndstooth_carpet/variant_" + variant);
        }
    }

    private HoundstoothCarpetModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        if (ModList.get().isLoaded("manors_bounty")) {
            for (ResourceLocation variant : VARIANTS) {
                event.register(variant);
            }
        }
    }

    @SubscribeEvent
    public static void installModels(ModelEvent.ModifyBakingResult event) {
        if (!ModList.get().isLoaded("manors_bounty")
                || !(BuiltInRegistries.BLOCK.get(CARPET) instanceof HoundstoothInfiniteCarpetBlock carpet)) {
            return;
        }

        var models = event.getModels();
        BakedModel missing = models.get(ModelBakery.MISSING_MODEL_LOCATION);
        BakedModel[] variants = new BakedModel[VARIANTS.length];
        for (int i = 0; i < VARIANTS.length; i++) {
            BakedModel model = models.get(VARIANTS[i]);
            if (model == null || model == missing) {
                baked = false;
                return;
            }
            variants[i] = model;
        }
        for (BlockState state : carpet.getStateDefinition().getPossibleStates()) {
            if (state.getValue(HoundstoothInfiniteCarpetBlock.ANIMATION) == 0) {
                int variant = state.getValue(HoundstoothInfiniteCarpetBlock.BLOCKSTATE);
                models.put(BlockModelShaper.stateToModelLocation(CARPET, state), variants[variant]);
            }
        }
        baked = true;
    }

    public static boolean hasBakedModels() {
        return baked;
    }
}
