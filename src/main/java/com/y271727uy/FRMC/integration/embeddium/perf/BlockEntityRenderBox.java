package com.y271727uy.FRMC.integration.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

/**
 * 方块实体渲染包围盒缓存。Forge 的默认 getRenderBoundingBox 每次调用都重新取碰撞箱并新建 AABB，
 * 而 Embeddium 每帧对每个可见方块实体调用一次；对没有重写该方法的方块实体，结果只取决于方块状态和位置，可以缓存。
 * 思路来自 Emberbium（Embeddium 的 fork，LGPL-3.0）。
 */
public final class BlockEntityRenderBox {
    /** 由 BlockEntityRenderBoxMixin 实现。 */
    public interface Holder {
        BlockState frmc$renderBoxState();

        AABB frmc$renderBox();

        void frmc$setRenderBox(BlockState state, AABB box);
    }

    /** 重写了 getRenderBoundingBox 的方块实体类，其结果可能随动画变化，不能缓存。 */
    private static final ClassValue<Boolean> USES_FORGE_DEFAULT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getRenderBoundingBox").getDeclaringClass() == IForgeBlockEntity.class;
            } catch (ReflectiveOperationException | LinkageError e) {
                return Boolean.FALSE;
            }
        }
    };

    private BlockEntityRenderBox() {
    }

    public static AABB get(BlockEntity blockEntity, Supplier<AABB> original) {
        if (!EmbeddiumPortConfig.blockEntityBoxCache() || !USES_FORGE_DEFAULT.get(blockEntity.getClass())) {
            return original.get();
        }

        BlockState state = blockEntity.getBlockState();
        // 潜影盒、移动中的活塞等：碰撞箱随方块实体内部状态变化，同一个方块状态下也会变。
        if (state.getBlock().hasDynamicShape()) {
            return original.get();
        }

        Holder holder = (Holder) blockEntity;
        AABB box = holder.frmc$renderBox();
        if (box == null || holder.frmc$renderBoxState() != state) {
            box = original.get();
            holder.frmc$setRenderBox(state, box);
        }
        return box;
    }
}
