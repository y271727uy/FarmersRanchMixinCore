package com.y271727uy.FRMC.client.jade;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.vinery.GrapevinePressingBatch;
import com.y271727uy.FRMC.integration.vinery.TavernPressing;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.satisfy.vinery.core.block.state.properties.GrapeProperty;
import net.satisfy.vinery.core.registry.ObjectRegistry;
import net.satisfy.vinery.core.util.GrapeType;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GrapevinePotJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation ID = new ResourceLocation(FRMCMod.MODID, "grapevine_pot");
    private static final String BATCH_DATA = "frmc_grapevine_pot";
    private static final String TEXT = "tooltip.frmc.grapevine_pot.";
    private static final int FIRST_PRESSING_STAGE = 3;
    private static final int READY_STAGE = 6;
    private static final int NATIVE_CAPACITY = 6;
    private static final int GRAPES_PER_BOTTLE = 3;

    @Override
    public ResourceLocation getUid() {
        return ID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof GrapevinePressingBatch batch && batch.totalInputs() > 0) {
            CompoundTag info = new CompoundTag();
            info.putInt("Total", batch.totalInputs());
            info.put("Fruit", batch.fruit().save(new CompoundTag()));
            info.put("Container", batch.container().save(new CompoundTag()));
            info.put("Result", batch.result().save(new CompoundTag()));
            data.put(BATCH_DATA, info);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        Property<?> stageProperty = state.getBlock().getStateDefinition().getProperty("stage");
        Property<?> storageProperty = state.getBlock().getStateDefinition().getProperty("storage");
        if (!(stageProperty instanceof IntegerProperty stageValue) || !(storageProperty instanceof IntegerProperty storageValue)) {
            return;
        }

        int stage = state.getValue(stageValue);
        int stored = state.getValue(storageValue);
        if (stored == 0) {
            tooltip.add(Component.translatable(TEXT + "empty"));
            return;
        }

        Contents contents = contents(state, accessor.getServerData(), stored);
        if (stage < READY_STAGE) {
            Component count = contents.capacity() > 0
                ? Component.translatable(TEXT + "loaded", stored, contents.capacity())
                : Component.translatable(TEXT + "loaded_unknown", stored);
            addItemLine(tooltip, contents.fruit(), count);
            if (stage >= FIRST_PRESSING_STAGE) {
                tooltip.add(Component.translatable(TEXT + "pressing", stage - FIRST_PRESSING_STAGE,
                    READY_STAGE - FIRST_PRESSING_STAGE));
            } else {
                tooltip.add(Component.translatable(TEXT + "filling"));
            }
        }

        if (!contents.result().isEmpty()) {
            ItemStack result = contents.result();
            addItemLine(tooltip, result, Component.translatable(TEXT + "result", result.getHoverName(), result.getCount()));
        }
        if (stage == READY_STAGE) {
            if (!contents.container().isEmpty()) {
                ItemStack container = contents.container();
                addItemLine(tooltip, container, Component.translatable(TEXT + "collect", container.getHoverName()));
            } else {
                tooltip.add(Component.translatable(TEXT + (contents.capacity() == 0 ? "ready" : "collect_empty_hand")));
            }
        }
    }

    private static Contents contents(BlockState state, CompoundTag data, int stored) {
        Property<?> pressingProperty = state.getBlock().getStateDefinition().getProperty("frmc_pressing");
        if (pressingProperty != null && state.getValue(pressingProperty) instanceof TavernPressing pressing) {
            if (pressing == TavernPressing.DATA_PACK) {
                if (data.contains(BATCH_DATA, Tag.TAG_COMPOUND)) {
                    CompoundTag info = data.getCompound(BATCH_DATA);
                    return new Contents(info.getInt("Total"), ItemStack.of(info.getCompound("Fruit")),
                        ItemStack.of(info.getCompound("Container")), ItemStack.of(info.getCompound("Result")));
                }
                return new Contents(0, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
            }
            if (pressing != TavernPressing.NONE) {
                return new Contents(TavernPressing.BATCH_SIZE, ItemStack.EMPTY, new ItemStack(Items.BUCKET), pressing.output());
            }
        }

        Property<?> grapeProperty = state.getBlock().getStateDefinition().getProperty("type");
        if (grapeProperty instanceof GrapeProperty type) {
            GrapeType grape = state.getValue(type);
            return new Contents(NATIVE_CAPACITY, new ItemStack(grape.getFruit()),
                new ItemStack(ObjectRegistry.WINE_BOTTLE.get()), new ItemStack(grape.getBottle(), stored / GRAPES_PER_BOTTLE));
        }
        return new Contents(0, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    private static void addItemLine(ITooltip tooltip, ItemStack stack, Component text) {
        if (stack.isEmpty()) {
            tooltip.add(text);
        } else {
            tooltip.add(tooltip.getElementHelper().smallItem(stack.copyWithCount(1)));
            tooltip.append(text);
        }
    }

    private record Contents(int capacity, ItemStack fruit, ItemStack container, ItemStack result) {
    }
}
