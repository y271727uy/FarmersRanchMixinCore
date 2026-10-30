package com.y271727uy.FRMC.mixin.fastchunkgen;

import com.mojang.datafixers.types.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Forge compatibility for FastChunkGen 0.34's DFU redirect.
 *
 * FastChunkGen redirects {@link CompletableFuture#join()} in DFU's bootstrap-loaded
 * {@code Type} class to its mod class {@code CFUtil}. That owner is not visible from
 * the bootstrap class loader on Forge, so wait on the future without emitting a
 * {@code CompletableFuture.join()} call that FastChunkGen can redirect again.
 */
@Mixin(value = Type.class, priority = 2000)
public abstract class TypeMixin {
    @Redirect(
        method = "rewrite",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;",
            remap = false
        ),
        remap = false
    )
    private static <T> T frmc$useBootstrapVisibleJoin(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CompletionException(exception);
        } catch (ExecutionException exception) {
            throw new CompletionException(exception.getCause());
        }
    }
}
