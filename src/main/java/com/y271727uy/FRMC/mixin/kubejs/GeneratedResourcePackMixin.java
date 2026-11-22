package com.y271727uy.FRMC.mixin.kubejs;

import com.y271727uy.FRMC.integration.kubejs.GeneratedPackIndex;
import dev.latvian.mods.kubejs.DevProperties;
import dev.latvian.mods.kubejs.script.data.GeneratedResourcePack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

/** Checked against KubeJS 2001.6.5-build.26. */
@Mixin(value = GeneratedResourcePack.class, remap = false)
public abstract class GeneratedResourcePackMixin {
    @Unique
    private static final Predicate<Path> FRMC$ANY_PATH = path -> true;

    @Shadow @Final private PackType packType;

    @Unique
    private volatile GeneratedPackIndex frmc$index;

    @Shadow
    public abstract Map<ResourceLocation, ?> getGenerated();

    /**
     * Folder listings from a sorted per-namespace index instead of a walk over every generated entry.
     * {@code listResources} overrides a vanilla method, so production jars carry its SRG name.
     */
    @Inject(method = {"listResources", "m_8031_"}, at = @At("HEAD"), cancellable = true)
    private void frmc$listFromIndex(PackType type, String namespace, String path, PackResources.ResourceOutput output,
                                    CallbackInfo ci) {
        ci.cancel();
        if (type != packType) {
            return;
        }
        Map<ResourceLocation, ?> generated = getGenerated();
        GeneratedPackIndex index = frmc$index;
        if (index == null || !index.covers(generated)) {
            index = GeneratedPackIndex.build(generated);
            frmc$index = index;
        }
        index.list(generated, namespace, path.endsWith("/") ? path : path + "/", output);
    }

    /**
     * The startup scan of kubejs/assets and kubejs/data only prints file name warnings to the KubeJS console.
     * Keep it for script authors who enabled KubeJS debug info, skip it otherwise.
     */
    @Inject(method = "scanForInvalidFiles", at = @At("HEAD"), cancellable = true)
    private static void frmc$skipFileNameScan(String pathName, Path path, CallbackInfo ci) {
        if (!DevProperties.get().debugInfo) {
            ci.cancel();
        }
    }

    /**
     * Every file in kubejs/data is probed with {@code Files.isReadable} before it is added, which opens a file handle
     * on Windows. Regular files there are readable in practice; one that is not now becomes an empty entry with a
     * logged stack trace (KubeJS's own read fallback) instead of being silently left out.
     */
    @ModifyArg(method = "getGenerated", at = @At(value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;",
            ordinal = 2), require = 0)
    private Predicate<Path> frmc$skipReadableProbe(Predicate<Path> readable) {
        return FRMC$ANY_PATH;
    }
}
