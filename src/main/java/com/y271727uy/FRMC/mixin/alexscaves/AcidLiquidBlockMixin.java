package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.AcidBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LiquidBlock.class)
public abstract class AcidLiquidBlockMixin {
    // Forge's field_to_method coremod normally rewrites these reads to getFluid() before Mixin runs.
    @WrapOperation(method = {"isPathfindable", "skipRendering", "onPlace", "updateShape", "neighborChanged",
            "shouldSpreadLiquid", "pickupBlock", "getPickupSound"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target =
                    "Lnet/minecraft/world/level/block/LiquidBlock;fluid:Lnet/minecraft/world/level/material/FlowingFluid;"),
            require = 0)
    private FlowingFluid frmc$resolveAcidFluid(LiquidBlock block, Operation<FlowingFluid> original) {
        FlowingFluid fluid = original.call(block);
        // Forge's supplier constructor leaves the legacy field unset.
        return fluid == null && block instanceof AcidBlock ? block.getFluid() : fluid;
    }
}
