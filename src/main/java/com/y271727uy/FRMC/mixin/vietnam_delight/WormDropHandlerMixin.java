package com.y271727uy.FRMC.mixin.vietnam_delight;

import com.y271727uy.FRMC.integration.vietnam_delight.VietnamDelightWormDrop;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原方法在下雨时无条件扫周围群系，而且每次锄地都往标准输出刷调试日志。
 * 这里直接换掉两个入口：先掷骰，没中就不查群系；查的时候只用已经加载的区块。
 */
@Pseudo
@Mixin(targets = "net.mcreator.vietnamdelight.WormDropHandler", remap = false)
public abstract class WormDropHandlerMixin {
    private static Item earthworm = Items.AIR;

    @Inject(method = "onHoeTill", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$replaceHoeTill(BlockEvent.BlockToolModificationEvent event, CallbackInfo ci) {
        ci.cancel();
        if (!ToolActions.HOE_TILL.equals(event.getToolAction())) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || !isTillableDirt(event.getState()) || !level.isRaining()) {
            return;
        }
        if (VietnamDelightWormDrop.roll(level.getRandom()::nextInt, VietnamDelightWormDrop.HOE_TILL_ONE_IN) == 0) {
            return;
        }
        if (!nearLoadedRiver(level, event.getPos())) {
            return;
        }
        dropOne(level, event.getPos());
    }

    @Inject(method = "onBlockBreak", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$replaceBlockBreak(BlockEvent.BreakEvent event, CallbackInfo ci) {
        ci.cancel();
        if (!(event.getLevel() instanceof ServerLevel level) || !isDirtOrGrass(event.getState()) || !level.isRaining()) {
            return;
        }
        if (VietnamDelightWormDrop.roll(level.getRandom()::nextInt, VietnamDelightWormDrop.BREAK_ONE_IN) == 0) {
            return;
        }
        if (!nearLoadedRiver(level, event.getPos())) {
            return;
        }
        dropOne(level, event.getPos());
    }

    private static boolean isTillableDirt(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
    }

    private static boolean isDirtOrGrass(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL);
    }

    private static boolean nearLoadedRiver(ServerLevel level, BlockPos pos) {
        ChunkSource chunks = level.getChunkSource();
        int quartY = pos.getY() >> 2;
        return VietnamDelightWormDrop.nearRiver(pos.getX(), pos.getZ(), (x, z) -> {
            ChunkAccess chunk = chunks.getChunkNow(x >> 4, z >> 4);
            if (chunk == null) {
                return null;
            }
            return chunk.getNoiseBiome(x >> 2, quartY, z >> 2).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse(null);
        });
    }

    private static void dropOne(ServerLevel level, BlockPos pos) {
        Item item = earthworm();
        if (item == Items.AIR) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(item));
        entity.setDeltaMovement(
            (level.getRandom().nextDouble() - 0.5) * 0.1,
            0.2,
            (level.getRandom().nextDouble() - 0.5) * 0.1
        );
        level.addFreshEntity(entity);
    }

    private static Item earthworm() {
        if (earthworm != Items.AIR) {
            return earthworm;
        }
        Item resolved = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vietnam_delight", "earthworm"));
        if (resolved == null || resolved == Items.AIR) {
            return Items.AIR;
        }
        earthworm = resolved;
        return resolved;
    }
}
