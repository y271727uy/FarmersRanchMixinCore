package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.ref.WeakReference;

/** Shares the consumer reference and texture snapshot across Citadel bones using one AR builder. */
public interface ICitadelMeshSource {
    WeakReference<IAcceleratedVertexConsumer> frmc$meshConsumerReference();

    NativeImage frmc$meshTexture();

    boolean frmc$canSubmitGpuPoses();

    void frmc$selectGpuPose(int sharingIndex);
}
