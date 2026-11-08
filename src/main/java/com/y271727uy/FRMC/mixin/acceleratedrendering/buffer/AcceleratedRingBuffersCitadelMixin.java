package com.y271727uy.FRMC.mixin.acceleratedrendering.buffer;

import com.github.argon4w.acceleratedrendering.core.backends.buffers.MappedBuffer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedRingBuffers;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseBatch;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelGpuBuffers;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AcceleratedRingBuffers.Buffers.class, remap = false)
public abstract class AcceleratedRingBuffersCitadelMixin implements ICitadelGpuBuffers {
    @Shadow @Final private MappedBuffer sharingBuffer;
    @Shadow @Final private MutableInt sharing;
    @Unique private CitadelGpuPoseBatch frmc$poseBatch;

    @Override
    public CitadelGpuPoseBatch frmc$citadelPoseBatch() {
        if (frmc$poseBatch == null) {
            frmc$poseBatch = new CitadelGpuPoseBatch();
        }
        return frmc$poseBatch;
    }

    @Override
    public int frmc$reserveCitadelPoses(int count) {
        sharingBuffer.reserve(count * AcceleratedBufferBuilder.SHARING_SIZE);
        int first = sharing.intValue();
        sharing.add(count);
        return first;
    }

    @Inject(method = "bindTransformBuffers", at = @At("HEAD"))
    private void frmc$writeCitadelPoses(CallbackInfo callback) {
        if (frmc$poseBatch != null) {
            frmc$poseBatch.flush(sharingBuffer.getBufferHandle());
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void frmc$resetCitadelPoses(CallbackInfo callback) {
        if (frmc$poseBatch != null) {
            frmc$poseBatch.clear();
        }
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void frmc$deleteCitadelPoses(CallbackInfo callback) {
        if (frmc$poseBatch != null) {
            frmc$poseBatch.close();
            frmc$poseBatch = null;
        }
    }
}
