package com.y271727uy.FRMC.capability.displaywine;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Arrays;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.satisfy.vinery.core.block.WineBottleBlock;

public final class DisplayWineFakeModel {
    public static StateDefinition.Builder<Block, BlockState> append(
            StateDefinition.Builder<Block, BlockState> builder,
            Property<?>[] properties,
            Operation<StateDefinition.Builder<Block, BlockState>> original) {
        Property<?>[] all = Arrays.copyOf(properties, properties.length + 1);
        all[all.length - 1] = WineBottleBlock.FAKE_MODEL;
        return original.call(builder, all);
    }

    private DisplayWineFakeModel() {
    }
}
