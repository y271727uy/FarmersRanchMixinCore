package com.y271727uy.FRMC.mixin.kaleidoscopetavern.vinery;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.DrinkBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.capability.displaywine.DisplayWineFakeModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "satisfyu.vinery.block.DrinkBlock", remap = false)
public abstract class DrinkBlockFakeModelMixin {
    @WrapOperation(
            method = "createCountBlockStateDefinition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/StateDefinition$Builder;add([Lnet/minecraft/world/level/block/state/properties/Property;)Lnet/minecraft/world/level/block/state/StateDefinition$Builder;",
                    remap = true),
            require = 0)
    private StateDefinition.Builder<Block, BlockState> frmc$addFakeModel(
            StateDefinition.Builder<Block, BlockState> builder,
            Property<?>[] properties,
            Operation<StateDefinition.Builder<Block, BlockState>> original) {
        return DisplayWineFakeModel.append(builder, properties, original);
    }
}
