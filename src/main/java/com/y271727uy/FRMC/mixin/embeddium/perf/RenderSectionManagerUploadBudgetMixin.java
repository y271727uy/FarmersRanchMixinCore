package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.perf.ChunkUploadBudget;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给 collectChunkBuildResults 的取结果循环加一道每帧预算：超出预算后让循环提前结束，
 * 没取走的结果原样留在 buildResults 里，下一帧的 uploadChunks 接着取。
 *
 * <p>以下情况一律不限流，保持原版行为：
 * <ul>
 *   <li>预算设为 0；</li>
 *   <li>本帧要求立即更新，或有玩家改动方块触发的重要重建（原版对它们是阻塞等待并当帧上传）；</li>
 *   <li>销毁渲染器时清理未处理的结果，否则会漏掉释放堆外内存；</li>
 *   <li>积压超过 {@link ChunkUploadBudget#MAX_BACKLOG}。</li>
 * </ul>
 *
 * <p>各注入点都是 require = 0，任何一个没命中时状态都会退回“不限流”，不会出现只生效一半的情况。
 */
@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerUploadBudgetMixin {
    @Shadow
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    /** 默认不限流；只有 updateChunks 的注入确认本帧没有需要当帧完成的工作时才放开限流。 */
    @Unique
    private boolean frmc$uploadUnlimited = true;

    /** 只在一次 collectChunkBuildResults 调用期间为 true。 */
    @Unique
    private boolean frmc$uploadCollecting;

    @Unique
    private long frmc$uploadBudget;

    @Unique
    private long frmc$uploadedBytes;

    @Unique
    private int frmc$uploadTaken;

    @Unique
    private int frmc$uploadQueued;

    @Inject(method = "updateChunks", at = @At("HEAD"), require = 0)
    private void frmc$decideUploadBudget(boolean updateImmediately, CallbackInfo ci) {
        this.frmc$uploadUnlimited = updateImmediately
            || frmc$hasPending(ChunkUpdateType.IMPORTANT_REBUILD)
            || frmc$hasPending(ChunkUpdateType.IMPORTANT_SORT);
    }

    @Inject(method = "destroy", at = @At("HEAD"), require = 0)
    private void frmc$drainEverythingOnDestroy(CallbackInfo ci) {
        this.frmc$uploadUnlimited = true;
    }

    @Inject(method = "collectChunkBuildResults", at = @At("HEAD"), require = 0)
    private void frmc$beginCollect(CallbackInfoReturnable<?> cir) {
        this.frmc$uploadBudget = this.frmc$uploadUnlimited ? 0L : ChunkUploadBudget.bytesPerFrame();
        this.frmc$uploadCollecting = this.frmc$uploadBudget > 0L;
        this.frmc$uploadedBytes = 0L;
        this.frmc$uploadTaken = 0;
        this.frmc$uploadQueued = -1;
    }

    @WrapOperation(
        method = "collectChunkBuildResults",
        at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ConcurrentLinkedDeque;poll()Ljava/lang/Object;"),
        require = 0
    )
    private Object frmc$pollWithinBudget(ConcurrentLinkedDeque<?> results, Operation<Object> original) {
        if (this.frmc$uploadCollecting && this.frmc$uploadTaken > 0 && this.frmc$uploadedBytes >= this.frmc$uploadBudget) {
            if (this.frmc$uploadQueued < 0) {
                // 第一次超出预算时才数一次队列长度（ConcurrentLinkedDeque.size 是 O(n)）。
                this.frmc$uploadQueued = results.size();
            }
            if (this.frmc$uploadQueued <= ChunkUploadBudget.MAX_BACKLOG) {
                this.frmc$uploadCollecting = false;
                return null;
            }
            this.frmc$uploadQueued--;
        }

        Object result = original.call(results);
        if (result == null) {
            this.frmc$uploadCollecting = false;
        } else {
            this.frmc$uploadTaken++;
        }
        return result;
    }

    @WrapOperation(
        method = "collectChunkBuildResults",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/executor/ChunkJobResult;unwrap()Ljava/lang/Object;"
        ),
        require = 0
    )
    private Object frmc$countUploadBytes(ChunkJobResult<?> result, Operation<Object> original) {
        Object output = original.call(result);
        if (this.frmc$uploadCollecting && output instanceof ChunkBuildOutput built) {
            this.frmc$uploadedBytes += ChunkUploadBudget.sizeOf(built);
        }
        return output;
    }

    @Unique
    private boolean frmc$hasPending(ChunkUpdateType type) {
        ArrayDeque<RenderSection> queue = this.rebuildLists.get(type);
        return queue != null && !queue.isEmpty();
    }
}
