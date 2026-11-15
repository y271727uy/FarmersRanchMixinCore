package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.graphics.PixelBuffers;
import xaero.map.region.texture.RegionTexture;

@Mixin(value = RegionTexture.class, remap = false)
public abstract class RegionTextureUploadMixin {
    @WrapOperation(method = "writeToUnpackPBO(ILxaero/map/pool/buffer/PoolTextureDirectBufferUnit;Z)V",
            at = @At(value = "INVOKE", target = "Lxaero/map/graphics/PixelBuffers;glMapBuffer(II)Ljava/nio/ByteBuffer;"),
            require = 1, expect = 1, allow = 1)
    private ByteBuffer frmc$discardPreviousUpload(int target, int access, Operation<ByteBuffer> original) {
        if (access == GL15.GL_WRITE_ONLY && XaeroPerformanceConfig.orphanUploadPbo()) {
            // The following put() replaces the complete tile. Keep Xaero's GL/ARB
            // dispatch and null-map retry path instead of imposing a GL version.
            PixelBuffers.glBufferData(target, RegionTexture.PBO_UNPACK_LENGTH, GL15.GL_STREAM_DRAW);
        }
        return original.call(target, access);
    }
}
