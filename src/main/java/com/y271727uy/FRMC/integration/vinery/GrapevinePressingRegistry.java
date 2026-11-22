package com.y271727uy.FRMC.integration.vinery;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = FRMCMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GrapevinePressingRegistry {
    public static final ResourceLocation ID = new ResourceLocation(FRMCMod.MODID, "grapevine_pressing");
    public static final ResourceLocation POT = new ResourceLocation("vinery", "grapevine_pot");
    public static final RecipeType<GrapevinePressingRecipe> TYPE = RecipeType.simple(ID);
    public static final GrapevinePressingRecipe.Serializer SERIALIZER = new GrapevinePressingRecipe.Serializer();

    static BlockEntityType<GrapevinePressingBatch> batchType;

    private GrapevinePressingRegistry() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.RECIPE_TYPE, ID, () -> TYPE);
        event.register(Registries.RECIPE_SERIALIZER, ID, () -> SERIALIZER);
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            Block pot = ForgeRegistries.BLOCKS.getValue(POT);
            if (pot != null && pot != Blocks.AIR) {
                batchType = BlockEntityType.Builder.of(GrapevinePressingBatch::new, pot).build(null);
                helper.register(ID, batchType);
            }
        });
    }
}
