package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import com.y271727uy.FRMC.integration.embeddium.perf.BlockTintCache;
import java.util.Arrays;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 同一次 renderModel 内，染色索引相同的面只向 BlockColor 查一次颜色。
 * BlockRenderer 每个构建线程一个实例，所以缓存放实例字段即可。
 */
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class BlockRendererTintCacheMixin {
    @Unique
    private Object frmc$tintProvider;

    @Unique
    private int frmc$tintIndex;

    @Unique
    private int frmc$tintColor;

    @Inject(method = "renderModel", at = @At("HEAD"), require = 0)
    private void frmc$resetTintCache(CallbackInfo ci) {
        this.frmc$tintProvider = null;
    }

    @WrapOperation(
        method = "getVertexColors",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;getColors(Lme/jellysquid/mods/sodium/client/world/WorldSlice;Lnet/minecraft/core/BlockPos;Ljava/lang/Object;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;[I)V"
        ),
        require = 0
    )
    private void frmc$cachedTint(
        ColorProvider<Object> provider,
        WorldSlice world,
        BlockPos pos,
        Object state,
        ModelQuadView quad,
        int[] output,
        Operation<Void> original
    ) {
        if (!EmbeddiumPortConfig.blockTintCache() || !BlockTintCache.isCacheable(provider)) {
            original.call(provider, world, pos, state, quad, output);
            return;
        }

        int tintIndex = quad.getColorIndex();
        if (this.frmc$tintProvider == provider && this.frmc$tintIndex == tintIndex) {
            Arrays.fill(output, this.frmc$tintColor);
            return;
        }

        original.call(provider, world, pos, state, quad, output);
        this.frmc$tintProvider = provider;
        this.frmc$tintIndex = tintIndex;
        this.frmc$tintColor = output[0];
    }
}
