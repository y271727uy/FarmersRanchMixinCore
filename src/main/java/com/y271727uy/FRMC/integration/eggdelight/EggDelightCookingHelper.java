package com.y271727uy.FRMC.integration.eggdelight;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Shared fast path for EggDelight's generated raw-omelet cooking procedures. */
public final class EggDelightCookingHelper {
    private static final net.minecraft.tags.TagKey<Block> HEAT_SOURCES =
        BlockTags.create(new ResourceLocation("farmersdelight", "heat_sources"));
    private static final net.minecraft.tags.TagKey<Block> TRAY_HEAT_SOURCES =
        BlockTags.create(new ResourceLocation("farmersdelight", "tray_heat_sources"));

    private EggDelightCookingHelper() {
    }

    public static void cook(LevelAccessor level, double x, double y, double z, Block rawBlock, Block cookedBlock,
                            boolean tray) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(tray ? TRAY_HEAT_SOURCES : HEAT_SOURCES)) {
            return;
        }

        BlockState current = level.getBlockState(pos);
        if (current.getBlock() != rawBlock) {
            return;
        }

        BlockState cooked = cookedBlock.defaultBlockState();
        cooked = copyMatchingProperties(current, cooked);
        level.setBlock(pos, cooked, 3);
    }

    private static BlockState copyMatchingProperties(BlockState source, BlockState target) {
        for (Property<?> sourceProperty : source.getProperties()) {
            Property<?> targetProperty = target.getBlock().getStateDefinition()
                .getProperty(sourceProperty.getName());
            if (targetProperty != null) {
                target = copyProperty(source, target, sourceProperty, targetProperty);
            }
        }
        return target;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState source, BlockState target,
                                           Property<?> sourceProperty, Property<?> targetProperty) {
        Comparable value = source.getValue((Property) sourceProperty);
        if (targetProperty.getValueClass().isInstance(value)) {
            return target.setValue((Property) targetProperty, value);
        }
        return target;
    }
}
