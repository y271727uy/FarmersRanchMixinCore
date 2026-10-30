package com.y271727uy.FRMC.mixin.vietnam_delight;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;
import net.huwng.vietnamdelight.block.DurianBlock;
import net.huwng.vietnamdelight.init.VietnamDelightModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = DurianBlock.class, remap = false)
public abstract class DurianBlockTickMixin {
    @Unique private static final int FRMC_IDLE_RESCHEDULE = 20;
    @Unique private static final Predicate<LivingEntity> FRMC_ALIVE = entity -> entity != null && entity.isAlive();
    @Unique private static Method frmc$configure;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void frmc$tickWithoutLoadingColumns(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        ci.cancel();
        BlockState above = frmc$loadedState(level, pos.above());
        if (above == null || !frmc$canHangFrom(above)) {
            BlockState below = frmc$loadedState(level, pos.below());
            if (below != null
                    && (below.canBeReplaced() || below.getBlock() instanceof LeavesBlock)
                    && pos.getY() >= level.getMinBuildHeight()) {
                frmc$drop(level, pos, state);
            }
            return;
        }
        if (frmc$hasDurianBelow(level, pos)) {
            level.scheduleTick(pos, state.getBlock(), 10);
            return;
        }

        int x = pos.getX();
        int z = pos.getZ();
        ChunkSource chunks = level.getChunkSource();
        ChunkAccess chunk = chunks.getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            level.scheduleTick(pos, state.getBlock(), FRMC_IDLE_RESCHEDULE);
            return;
        }
        LivingEntity target = null;
        int clearDistance = 0;
        for (int distance = 1; distance <= 16; distance++) {
            BlockPos cursor = pos.below(distance);
            BlockState below = chunk.getBlockState(cursor);
            if (!below.isAir() && !(below.getBlock() instanceof LeavesBlock) && !below.canBeReplaced()) {
                break;
            }
            clearDistance = distance;
        }
        if (clearDistance > 0) {
            AABB box = new AABB(
                    x,
                    pos.getY() - clearDistance,
                    z,
                    x + 1.0,
                    pos.getY() + 1.0,
                    z + 1.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box, FRMC_ALIVE);
            if (!entities.isEmpty()) {
                target = entities.get(0);
            }
        }
        if (target != null) {
            frmc$drop(level, pos, state);
        } else {
            level.scheduleTick(pos, state.getBlock(), FRMC_IDLE_RESCHEDULE);
        }
    }

    @Unique
    private static boolean frmc$canHangFrom(BlockState state) {
        Block block = state.getBlock();
        return block == VietnamDelightModBlocks.DURIAN_LOG.get()
                || block == VietnamDelightModBlocks.DURIAN_WOOD.get()
                || block == VietnamDelightModBlocks.STRIPPED_DURIAN_LOG.get()
                || block == VietnamDelightModBlocks.STRIPPED_DURIAN_WOOD.get()
                || state.is(BlockTags.LEAVES);
    }

    @Unique
    private static boolean frmc$hasDurianBelow(ServerLevel level, BlockPos pos) {
        int limit = Math.min(24, pos.getY() - level.getMinBuildHeight());
        ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return false;
        }
        Block durian = VietnamDelightModBlocks.DURIAN.get();
        for (int distance = 1; distance <= limit; distance++) {
            BlockState state = chunk.getBlockState(pos.below(distance));
            if (state.getBlock() instanceof DurianBlock || state.is(durian)) {
                return true;
            }
            if (!state.isAir() && !(state.getBlock() instanceof LeavesBlock) && !state.canBeReplaced()) {
                return false;
            }
        }
        return false;
    }

    @Unique
    private void frmc$drop(ServerLevel level, BlockPos pos, BlockState state) {
        FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, state);
        try {
            frmc$configure().invoke(this, falling);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Unique
    private static Method frmc$configure() {
        if (frmc$configure == null) {
            for (Method method : DurianBlock.class.getDeclaredMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == FallingBlockEntity.class) {
                    method.setAccessible(true);
                    frmc$configure = method;
                    break;
                }
            }
            if (frmc$configure == null) {
                throw new IllegalStateException("DurianBlock falling configurator is missing");
            }
        }
        return frmc$configure;
    }

    @Unique
    private static BlockState frmc$loadedState(ServerLevel level, BlockPos pos) {
        ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
