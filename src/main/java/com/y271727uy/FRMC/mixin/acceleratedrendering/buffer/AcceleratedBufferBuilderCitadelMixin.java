package com.y271727uy.FRMC.mixin.acceleratedrendering.buffer;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelMeshSource;
import java.lang.ref.WeakReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(value = AcceleratedBufferBuilder.class, remap = false)
public abstract class AcceleratedBufferBuilderCitadelMixin implements ICitadelMeshSource {
    @Shadow private int activeSharing;
    @Shadow private long sharingAddress;
    @Unique
    private WeakReference<IAcceleratedVertexConsumer> frmc$meshConsumerReference;
    @Unique
    private WeakReference<NativeImage> frmc$meshTexture;

    @Override
    public WeakReference<IAcceleratedVertexConsumer> frmc$meshConsumerReference() {
        if (frmc$meshConsumerReference == null) {
            frmc$meshConsumerReference = new WeakReference<>((IAcceleratedVertexConsumer) (Object) this);
        }
        return frmc$meshConsumerReference;
    }

    @Override
    public NativeImage frmc$meshTexture() {
        if (frmc$meshTexture == null) {
            AcceleratedBufferBuilder builder = (AcceleratedBufferBuilder) (Object) this;
            frmc$meshTexture = new WeakReference<>(builder.downloadTexture());
        }
        return frmc$meshTexture.get();
    }

    @Override
    public boolean frmc$canSubmitGpuPoses() {
        return activeSharing == -1;
    }

    @Override
    public void frmc$selectGpuPose(int sharingIndex) {
        activeSharing = sharingIndex;
        sharingAddress = -1L;
    }
}
