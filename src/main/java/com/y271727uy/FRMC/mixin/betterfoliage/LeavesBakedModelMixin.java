package com.y271727uy.FRMC.mixin.betterfoliage;

import com.eerussianguy.betterfoliage.model.LeavesBakedModel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.capability.betterfoliagecutting.BetterFoliageInteriorCulling;
import com.y271727uy.FRMC.capability.betterfoliagecutting.ChlorideLeavesSupport;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 仅当 Chloride 实际在剔叶时动手（ALL，或 Embeddium Fast leaves）；关剔除则 BF 原样。
 * Chloride ALL 只跳过立方体面。BF fluff 十字挂在 NORTH/SOUTH 上，会穿过空心树冠。
 * 六邻都是叶：所有方向含 {@code dir == null} 返回空。
 * 北或南任一侧邻叶：任意方向丢掉 crosses，只留 core（overlay 也留）。
 */
@Pseudo
@Mixin(targets = "com.eerussianguy.betterfoliage.model.LeavesBakedModel", remap = false)
public abstract class LeavesBakedModelMixin {
    @Unique
    private static final ModelProperty<Integer> FRMC$LEAF_NEIGHBOR_MASK = new ModelProperty<>();

    @Shadow
    private BakedModel core;

    @Shadow
    private BakedModel outerCore;

    @Shadow
    @Final
    private boolean isOverlay;

    @ModifyReturnValue(method = "getModelData", at = @At("RETURN"))
    private ModelData frmc$attachLeafNeighborMask(
        ModelData original,
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        ModelData extra
    ) {
        int mask = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (ChlorideLeavesSupport.isOccludingLeaf(level.getBlockState(cursor.setWithOffset(pos, direction)).getBlock())) {
                mask |= 1 << direction.get3DDataValue();
            }
        }
        return original.derive().with(FRMC$LEAF_NEIGHBOR_MASK, Integer.valueOf(mask)).build();
    }

    @Inject(method = "getQuads", at = @At("HEAD"), cancellable = true)
    private void frmc$cullInteriorAndInwardCrosses(
        BlockState state,
        Direction side,
        RandomSource rand,
        ModelData data,
        RenderType renderType,
        CallbackInfoReturnable<List<BakedQuad>> cir
    ) {
        if (!ChlorideLeavesSupport.isLeavesCullingActive()) {
            return;
        }
        Integer maskBoxed = data.get(FRMC$LEAF_NEIGHBOR_MASK);
        if (maskBoxed == null) {
            return;
        }
        int mask = maskBoxed.intValue();
        if (BetterFoliageInteriorCulling.shouldCullEntirely(mask)) {
            cir.setReturnValue(List.of());
            return;
        }
        if (!BetterFoliageInteriorCulling.shouldDropCrosses(mask)) {
            return;
        }
        cir.setReturnValue(frmc$coreAndOverlayQuads(state, side, rand, data, renderType));
    }

    @Unique
    private List<BakedQuad> frmc$coreAndOverlayQuads(
        BlockState state,
        Direction side,
        RandomSource rand,
        ModelData data,
        RenderType renderType
    ) {
        List<BakedQuad> quads = this.core.getQuads(state, side, rand, data, renderType);
        if (!this.isOverlay || this.outerCore == null) {
            return quads;
        }
        List<BakedQuad> overlayQuads = this.outerCore.getQuads(state, side, rand, data, renderType);
        if (overlayQuads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> merged = new ArrayList<>(quads.size() + overlayQuads.size());
        merged.addAll(quads);
        merged.addAll(overlayQuads);
        return merged;
    }
}
