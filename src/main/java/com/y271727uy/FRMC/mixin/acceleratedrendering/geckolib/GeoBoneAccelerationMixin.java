package com.y271727uy.FRMC.mixin.acceleratedrendering.geckolib;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.data.MeshData;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.geckolib.renderer.GeckoCubePose;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

@Mixin(value = GeoBone.class, remap = false)
public abstract class GeoBoneAccelerationMixin implements IAcceleratedRenderer<Void> {
    @Shadow @Final private List<GeoCube> cubes;

    @Unique private final Map<IAcceleratedVertexConsumer, IMesh> frmc$meshes = new HashMap<>();
    @Unique private final Map<MeshData, IMesh> frmc$mergedMeshes = new HashMap<>();

    @Override
    public void render(VertexConsumer buffer, Void context, Matrix4f pose, Matrix3f normal,
                       int light, int overlay, int color) {
        var accelerated = VertexConsumerExtension.getAccelerated(buffer);
        IMesh mesh = frmc$meshes.get(accelerated);
        if (mesh == null) {
            mesh = frmc$buildMesh(accelerated);
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
    private IMesh frmc$buildMesh(IAcceleratedVertexConsumer accelerated) {
        var collector = CoreFeature.createMeshCollector(accelerated);
        VertexConsumer writer = accelerated.decorate(collector);
        PoseStack cubePose = new PoseStack();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        for (GeoCube cube : cubes) {
            cubePose.pushPose();
            GeckoCubePose.rotate(cubePose, cube);
            PoseStack.Pose pose = cubePose.last();
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                quad.normal().mul(pose.normal(), normal);
                for (GeoVertex vertex : quad.vertices()) {
                    vertex.position().mulPosition(pose.pose(), position);
                    writer.vertex(position.x, position.y, position.z, 1, 1, 1, 1,
                            vertex.texU(), vertex.texV(), 0, 0, normal.x, normal.y, normal.z);
                }
            }
            cubePose.popPose();
        }
        collector.flush();

        MeshData data = collector.getData();
        IMesh cached = frmc$mergedMeshes.get(data);
        if (cached != null) {
            collector.getBuffer().close();
            return cached;
        }

        IMesh mesh = AcceleratedEntityRenderingFeature.getMeshType().getBuilder().build(collector);
        frmc$mergedMeshes.put(data, mesh);
        return mesh;
    }
}
