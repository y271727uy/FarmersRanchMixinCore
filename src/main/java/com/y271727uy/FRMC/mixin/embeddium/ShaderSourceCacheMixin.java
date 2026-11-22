package com.y271727uy.FRMC.mixin.embeddium;

import com.y271727uy.FRMC.integration.embeddium.EmbeddiumPortConfig;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embeddium reads chunk shader sources (and their {@code #import}s) with {@code ClassLoader#getResourceAsStream},
 * not through resource packs, so a renderer reload searches every mod jar again for every file. Jar contents are
 * fixed for the session, so the first successful read is reused. Missing shaders still throw as before.
 * Identical in Embeddium 0.3.31 and Emberbium, hence outside the {@code perf} group.
 */
@Mixin(value = ShaderLoader.class, remap = false)
public abstract class ShaderSourceCacheMixin {
    @Unique
    private static final Map<ResourceLocation, String> FRMC$SOURCES = new ConcurrentHashMap<>();

    @Inject(method = "getShaderSource", at = @At("HEAD"), cancellable = true)
    private static void frmc$reuseShaderSource(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        if (name != null && EmbeddiumPortConfig.shaderSourceCache()) {
            String cached = FRMC$SOURCES.get(name);
            if (cached != null) {
                cir.setReturnValue(cached);
            }
        }
    }

    @Inject(method = "getShaderSource", at = @At("RETURN"))
    private static void frmc$rememberShaderSource(ResourceLocation name, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        if (name != null && source != null && EmbeddiumPortConfig.shaderSourceCache()) {
            FRMC$SOURCES.putIfAbsent(name, source);
        }
    }
}
