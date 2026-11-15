package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.perf.BlockEntityRenderBox;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public abstract class BlockEntityRenderBoxMixin implements BlockEntityRenderBox.Holder {
    @Unique private BlockState frmc$renderBoxState;
    @Unique private AABB frmc$renderBox;

    @Override
    public BlockState frmc$renderBoxState() {
        return this.frmc$renderBoxState;
    }

    @Override
    public AABB frmc$renderBox() {
        return this.frmc$renderBox;
    }

    @Override
    public void frmc$setRenderBox(BlockState state, AABB box) {
        this.frmc$renderBoxState = state;
        this.frmc$renderBox = box;
    }
}
