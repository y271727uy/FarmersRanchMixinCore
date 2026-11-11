package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.AbstractTrailParticle;
import com.github.alexmodguy.alexscaves.client.particle.MagneticOrbitParticle;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.alexscaves.MagneticParticleBatch;
import com.y271727uy.FRMC.integration.citadel.render.embeddium.CitadelEmbeddiumRenderer;
import com.y271727uy.FRMC.integration.embeddium.vertex.EntityVertex;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTrailParticle.class)
public abstract class MagneticOrbitVertexMixin extends Particle {
    @Shadow(remap = false) private int trailPointer;
    @Shadow(remap = false) protected float trailR;
    @Shadow(remap = false) protected float trailG;
    @Shadow(remap = false) protected float trailB;
    @Shadow(remap = false) protected float trailA;
    @Shadow(remap = false) protected abstract VertexConsumer getVetrexConsumer(MultiBufferSource.BufferSource buffers);

    protected MagneticOrbitVertexMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$writeOrbitTrail(VertexConsumer unused, Camera camera, float partialTick, CallbackInfo ci) {
        if (!((Object) this instanceof MagneticOrbitParticle orbit) || trailPointer < 0) {
            return;
        }
        var buffers = MagneticParticleBatch.getOrbitBuffers();
        int samples = orbit.sampleCount();
        if (buffers == null || samples <= 0 || samples > 64) {
            return;
        }
        VertexConsumer consumer = getVetrexConsumer(buffers);
        if (!CitadelEmbeddiumRenderer.isSupported(consumer) || CitadelEmbeddiumRenderer.isArAccelerated(consumer)) {
            return;
        }
        VertexBufferWriter writer = VertexBufferWriter.of(consumer);
        Vec3 cameraPosition = camera.getPosition();
        Vec3 from = new Vec3((float) Mth.lerp((double) partialTick, xo, x),
                (float) Mth.lerp((double) partialTick, yo, y), (float) Mth.lerp((double) partialTick, zo, z));
        float rotation = orbit.getTrailRot(camera);
        float halfHeight = orbit.getTrailHeight() / 2.0F;
        Vec3 top = new Vec3(0, halfHeight, 0).zRot(rotation);
        Vec3 bottom = new Vec3(0, -halfHeight, 0).zRot(rotation);
        int color = ColorABGR.pack((int) (trailR * 255), (int) (trailG * 255),
                (int) (trailB * 255), (int) (trailA * 255));
        int light = getLightColor(partialTick);
        int normal = NormI8.pack(0, 1, 0);
        int step = orbit.sampleStep();
        int count = samples * 4;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long vertices = stack.nmalloc(4, count * EntityVertex.STRIDE);
            long ptr = vertices;
            for (int segment = 0; segment < samples; segment++) {
                Vec3 to = orbit.getTrailPosition(segment * step, partialTick);
                float u1 = segment / (float) samples;
                float u2 = u1 + 1.0F / samples;
                ptr = frmc$writeTrailVertex(ptr, from, bottom, cameraPosition, color, u1, 1, light, normal);
                ptr = frmc$writeTrailVertex(ptr, to, bottom, cameraPosition, color, u2, 1, light, normal);
                ptr = frmc$writeTrailVertex(ptr, to, top, cameraPosition, color, u2, 0, light, normal);
                ptr = frmc$writeTrailVertex(ptr, from, top, cameraPosition, color, u1, 0, light, normal);
                from = to;
            }
            writer.push(stack, vertices, count, EntityVertex.FORMAT);
        }
        ci.cancel();
    }

    @Unique
    private static long frmc$writeTrailVertex(long ptr, Vec3 sample, Vec3 edge, Vec3 camera,
            int color, float u, float v, int light, int normal) {
        // Match the float addition order of the original world coordinates and PoseStack translation.
        EntityVertex.write(ptr, (float) sample.x + (float) edge.x - (float) camera.x,
                (float) sample.y + (float) edge.y - (float) camera.y,
                (float) sample.z + (float) edge.z - (float) camera.z,
                color, u, v, OverlayTexture.NO_OVERLAY, light, normal);
        return ptr + EntityVertex.STRIDE;
    }
}
