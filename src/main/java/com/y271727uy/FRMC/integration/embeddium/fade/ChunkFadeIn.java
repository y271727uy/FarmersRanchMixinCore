package com.y271727uy.FRMC.integration.embeddium.fade;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderParser;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.render.ShaderModBridge;
import org.lwjgl.opengl.GL20C;
import org.slf4j.Logger;
import javax.annotation.Nullable;

/**
 * 区块淡入：新加载的区块在 {@link #DURATION_MILLIS} 毫秒内从雾色过渡到正常颜色。
 * 思路移植自 Emberbium（Embeddium 的 fork，LGPL-3.0）；这里不改 Embeddium 的着色器选项，
 * 而是给正在淡入的 region 单独编译一份打过补丁的区块着色器，其余 region 仍走原程序，零额外开销。
 */
public final class ChunkFadeIn {
    public static final int DURATION_MILLIS = 750;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation VERTEX_SOURCE = new ResourceLocation("sodium", "blocks/block_layer_opaque.vsh");
    private static final ResourceLocation FRAGMENT_SOURCE = new ResourceLocation("sodium", "blocks/block_layer_opaque.fsh");

    // 每个 region 256 个 section，每个 section 一个进度值，按 vec4[64] 上传。
    private static final String VERTEX_DECLARATIONS = "uniform vec4 u_FadeIn[64];\nflat out float v_FadeIn;\n\n";
    private static final String VERTEX_ASSIGNMENT = "\n    v_FadeIn = u_FadeIn[int(_draw_id >> 2u)][int(_draw_id & 3u)];";
    private static final String FRAGMENT_DECLARATIONS = "flat in float v_FadeIn;\n\n";
    private static final String FRAGMENT_BLEND = "    fragColor.rgb = mix(u_FogColor.rgb, fragColor.rgb, v_FadeIn);\n";

    /** 着色器补丁或编译失败后永久关闭，避免每帧重试。 */
    private static boolean broken;

    /** 渲染器重载期间暂存“已经淡入过”的 section，使 F3+A 或改设置不会让整个世界重新淡入。仅渲染线程访问。 */
    @Nullable
    public static LongSet settledSections;

    private ChunkFadeIn() {
    }

    public static boolean active() {
        return !broken && EmbeddiumPortConfig.chunkFadeIn() && !ShaderModBridge.areShadersEnabled();
    }

    public static int now() {
        return (int) (System.nanoTime() / 1_000_000L);
    }

    public static float progress(int start, int now) {
        float t = Math.max(0.0f, Math.min((now - start) / (float) DURATION_MILLIS, 1.0f));
        return t * t * (3.0f - 2.0f * t);
    }

    /** 淡入专用的区块着色器程序及其 u_FadeIn 位置。 */
    public record Program(GlProgram<ChunkShaderInterface> program, int fadeLocation) {
        public void uploadProgress(float[] progress) {
            GL20C.glUniform4fv(this.fadeLocation, progress);
        }
    }

    @Nullable
    public static Program createProgram(ChunkShaderOptions options) {
        try {
            return compile(options);
        } catch (RuntimeException e) {
            broken = true;
            LOGGER.error("[FRMC] Chunk fade-in disabled: could not build the fade-in variant of Embeddium's chunk shader", e);
            return null;
        }
    }

    private static Program compile(ChunkShaderOptions options) {
        ShaderConstants constants = options.constants();

        GlShader vertex = new GlShader(ShaderType.VERTEX, VERTEX_SOURCE,
            ShaderParser.parseShader(patchVertex(ShaderLoader.getShaderSource(VERTEX_SOURCE)), constants));
        GlShader fragment;
        try {
            fragment = new GlShader(ShaderType.FRAGMENT, FRAGMENT_SOURCE,
                ShaderParser.parseShader(patchFragment(ShaderLoader.getShaderSource(FRAGMENT_SOURCE)), constants));
        } catch (RuntimeException e) {
            vertex.delete();
            throw e;
        }

        GlProgram<ChunkShaderInterface> program;
        try {
            // 与 ShaderChunkRenderer.createShader 保持一致的属性绑定。
            program = GlProgram.builder(new ResourceLocation("frmc", "chunk_shader_fade_in"))
                .attachShader(vertex)
                .attachShader(fragment)
                .bindAttribute("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                .link(shader -> new ChunkShaderInterface(shader, options));
        } finally {
            vertex.delete();
            fragment.delete();
        }

        int location = GL20C.glGetUniformLocation(program.handle(), "u_FadeIn");
        if (location < 0) {
            program.delete();
            throw new IllegalStateException("u_FadeIn is missing from the linked program");
        }
        return new Program(program, location);
    }

    static String patchVertex(String source) {
        String declared = insertBefore(source, "void main()", VERTEX_DECLARATIONS);
        return insertAfter(declared, "_vert_init();", VERTEX_ASSIGNMENT);
    }

    static String patchFragment(String source) {
        String declared = insertBefore(source, "void main()", FRAGMENT_DECLARATIONS);
        int end = declared.lastIndexOf('}');
        if (end < 0) {
            throw new IllegalStateException("fragment shader has no closing brace");
        }
        return declared.substring(0, end) + FRAGMENT_BLEND + declared.substring(end);
    }

    private static String insertBefore(String source, String anchor, String text) {
        int index = anchorIndex(source, anchor);
        return source.substring(0, index) + text + source.substring(index);
    }

    private static String insertAfter(String source, String anchor, String text) {
        int index = anchorIndex(source, anchor) + anchor.length();
        return source.substring(0, index) + text + source.substring(index);
    }

    private static int anchorIndex(String source, String anchor) {
        int index = source.indexOf(anchor);
        if (index < 0 || source.indexOf(anchor, index + 1) >= 0) {
            throw new IllegalStateException("expected exactly one '" + anchor + "' in the chunk shader source");
        }
        return index;
    }
}
