package com.y271727uy.FRMC.mixin.embeddium.fade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.embeddium.fade.ChunkFadeIn;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeRegion;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 逐 region 绘制时，正在淡入的 region 临时换成淡入版着色器并上传各 section 的进度；
 * 没有 region 在淡入时，这里只多一次布尔判断。
 */
@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class DefaultChunkRendererMixin extends ShaderChunkRenderer {
    @Unique private final Reference2ReferenceOpenHashMap<TerrainRenderPass, ChunkFadeIn.Program> frmc$fadePrograms =
        new Reference2ReferenceOpenHashMap<>();
    @Unique private final float[] frmc$fadeProgress = new float[RenderRegion.REGION_SIZE];

    @Unique private boolean frmc$fadeActive;
    @Unique private int frmc$fadeNow;
    @Unique private ChunkFadeIn.Program frmc$boundFadeProgram;

    private DefaultChunkRendererMixin(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);
    }

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void frmc$beginFadePass(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderListIterable renderLists,
                                    TerrainRenderPass renderPass, CameraTransform camera, CallbackInfo ci) {
        this.frmc$fadeActive = ChunkFadeIn.active();
        this.frmc$fadeNow = this.frmc$fadeActive ? ChunkFadeIn.now() : 0;
        this.frmc$boundFadeProgram = null;
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;setModelMatrixUniforms(Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkShaderInterface;Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;Lme/jellysquid/mods/sodium/client/render/viewport/CameraTransform;)V"
        ),
        require = 0
    )
    private void frmc$selectFadeProgram(ChunkShaderInterface shader, RenderRegion region, CameraTransform camera,
                                        Operation<Void> original,
                                        @Local(argsOnly = true) ChunkRenderMatrices matrices,
                                        @Local(argsOnly = true) TerrainRenderPass renderPass) {
        if (this.frmc$fadeActive) {
            FadeRegion fadeRegion = (FadeRegion) (Object) region;
            ChunkFadeIn.Program wanted = fadeRegion.frmc$isFadingIn(this.frmc$fadeNow)
                ? this.frmc$fadeProgram(renderPass)
                : null;

            if (wanted != this.frmc$boundFadeProgram) {
                if (wanted != null) {
                    wanted.program().bind();

                    ChunkShaderInterface fadeShader = wanted.program().getInterface();
                    fadeShader.setupState();
                    fadeShader.setProjectionMatrix(matrices.projection());
                    fadeShader.setModelViewMatrix(matrices.modelView());
                } else {
                    // 回到 begin() 绑定的原程序，它的 uniform 仍然有效。
                    this.activeProgram.bind();
                }
                this.frmc$boundFadeProgram = wanted;
            }

            if (wanted != null) {
                fadeRegion.frmc$writeFadeIn(this.frmc$fadeProgress, this.frmc$fadeNow);
                wanted.uploadProgress(this.frmc$fadeProgress);
                shader = wanted.program().getInterface();
            }
        }

        original.call(shader, region, camera);
    }

    @Unique
    private ChunkFadeIn.Program frmc$fadeProgram(TerrainRenderPass renderPass) {
        ChunkFadeIn.Program program = this.frmc$fadePrograms.get(renderPass);

        if (program == null) {
            program = ChunkFadeIn.createProgram(new ChunkShaderOptions(ChunkFogMode.SMOOTH, renderPass, this.vertexType));

            if (program == null) {
                // 编译失败：本帧起不再尝试，全部走原程序。
                this.frmc$fadeActive = false;
                return null;
            }

            this.frmc$fadePrograms.put(renderPass, program);
        }

        return program;
    }

    @Inject(method = "delete", at = @At("HEAD"), require = 0)
    private void frmc$deleteFadePrograms(CommandList commandList, CallbackInfo ci) {
        for (ChunkFadeIn.Program program : this.frmc$fadePrograms.values()) {
            program.program().delete();
        }
        this.frmc$fadePrograms.clear();
    }
}
