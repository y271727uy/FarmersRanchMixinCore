package com.y271727uy.FRMC.mixin.acceleratedrendering.touhoulittlemaid;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import com.github.argon4w.acceleratedrendering.core.meshes.data.MeshData;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.acceleratedrendering.MaidBoneGeometry;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GeoBone.class, remap = false)
public abstract class GeoBoneAccelerationMixin implements IAcceleratedRenderer<AnimatedGeoBone> {
    @Unique private final Map<IAcceleratedVertexConsumer, IMesh> frmc$meshes = new HashMap<>();
    @Unique private final Map<MeshData, IMesh> frmc$mergedMeshes = new HashMap<>();
    @Unique private MeshType frmc$meshType;

    @Override
    public void render(VertexConsumer buffer, AnimatedGeoBone bone, Matrix4f pose, Matrix3f normal,
                       int light, int overlay, int color) {
        var accelerated = VertexConsumerExtension.getAccelerated(buffer);
        MeshType meshType = AcceleratedEntityRenderingFeature.getMeshType();
        if (meshType != frmc$meshType) {
            frmc$meshes.clear();
            frmc$mergedMeshes.clear();
            frmc$meshType = meshType;
        }
        IMesh mesh = frmc$meshes.get(accelerated);
        if (mesh == null) {
            mesh = frmc$buildMesh(accelerated, bone, meshType);
            frmc$meshes.put(accelerated, mesh);
        }

        accelerated.beginTransform(pose, normal);
        try {
            mesh.write(accelerated, color, light, overlay);
        } finally {
            accelerated.endTransform();
        }
    }

    @Unique
    private IMesh frmc$buildMesh(IAcceleratedVertexConsumer accelerated, AnimatedGeoBone bone, MeshType meshType) {
        var collector = CoreFeature.createMeshCollector(accelerated);
        try {
            MaidBoneGeometry.emit(bone, accelerated.decorate(collector));
            collector.flush();
        } catch (RuntimeException | Error failure) {
            collector.getBuffer().close();
            throw failure;
        }

        MeshData data = collector.getData();
        IMesh mesh = frmc$mergedMeshes.get(data);
        if (mesh != null) {
            collector.getBuffer().close();
            return mesh;
        }
        mesh = meshType.getBuilder().build(collector);
        frmc$mergedMeshes.put(data, mesh);
        return mesh;
    }
}
