package com.y271727uy.FRMC.mixin.acceleratedrendering.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.y271727uy.FRMC.integration.acceleratedrendering.citadel.CitadelArRenderer;
import com.y271727uy.FRMC.integration.acceleratedrendering.citadel.CitadelGeometryAccess;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.IMeshCollector;
import com.github.argon4w.acceleratedrendering.core.meshes.data.MeshData;
import com.github.argon4w.acceleratedrendering.core.utils.ByteBufferBuilder;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Map;
import net.minecraft.util.FastColor.ARGB32;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AdvancedModelBox.class, remap = false, priority = 900)
public abstract class AdvancedModelBoxMixin implements IAcceleratedRenderer<Void> {
    @Shadow
    public ObjectList<?> cubeList;

    @Unique
    private Map<IBufferGraph, IMesh> frmc$meshes;
    @Unique
    private Map<MeshData, IMesh> frmc$mergedMeshes;

    @Shadow
    public float scaleX;
    @Shadow
    public float scaleY;
    @Shadow
    public float scaleZ;
    @Shadow
    public ObjectList<BasicModelPart> childModels;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$renderIdentityEmptyParent(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                                 float red, float green, float blue, float alpha,
                                                 CallbackInfo callback) {
        BasicModelPart part = (BasicModelPart)(Object)this;
        if (!part.showModel || !cubeList.isEmpty() || childModels.isEmpty()
                || part.rotationPointX != 0.0F || part.rotationPointY != 0.0F || part.rotationPointZ != 0.0F
                || part.rotateAngleX != 0.0F || part.rotateAngleY != 0.0F || part.rotateAngleZ != 0.0F
                || scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F) {
            return;
        }

        for (int i = 0, size = childModels.size(); i < size; i++) {
            childModels.get(i).render(pose, buffer, light, overlay, red, green, blue, alpha);
        }
        callback.cancel();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", at = @At("HEAD"), remap = false)
    private void frmc$diagnosticBoneRender(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                           float red, float green, float blue, float alpha,
                                           CallbackInfo callback) {
        if (EntityRenderDiagnostics.enabled()) {
            EntityRenderDiagnostics.logBigCatBoneTree((AdvancedModelBox)(Object)this);
            EntityRenderDiagnostics.recordBoneRender();
            if (scaleX == 1.0F && scaleY == 1.0F && scaleZ == 1.0F) {
                EntityRenderDiagnostics.recordUnitScale();
            }
        }
    }

    @Unique
    private float frmc$rotationX;
    @Unique
    private float frmc$rotationY;
    @Unique
    private float frmc$rotationZ;
    @Unique
    private final Quaternionf frmc$identityQuaternion = new Quaternionf();
    @Unique
    private final Quaternionf frmc$combinedQuaternion = new Quaternionf();
    @Unique
    private float frmc$cachedRotationX;
    @Unique
    private float frmc$cachedRotationY;
    @Unique
    private float frmc$cachedRotationZ;
    @Unique
    private boolean frmc$hasCachedRotation;

    @Inject(method = "translateAndRotate", at = @At("HEAD"), remap = false)
    private void frmc$resetCombinedRotation(PoseStack pose, CallbackInfo callback) {
        frmc$rotationX = 0.0F;
        frmc$rotationY = 0.0F;
        frmc$rotationZ = 0.0F;
    }

    @Redirect(method = "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/math/Axis;rotation(F)Lorg/joml/Quaternionf;"), remap = false)
    private Quaternionf frmc$captureAxisRotation(Axis axis, float angle) {
        if (axis == Axis.XP) {
            frmc$rotationX = angle;
        } else if (axis == Axis.YP) {
            frmc$rotationY = angle;
        } else if (axis == Axis.ZP) {
            frmc$rotationZ = angle;
        }
        return frmc$identityQuaternion.set(0.0F, 0.0F, 0.0F, 1.0F);
    }

    @Redirect(method = "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V"), remap = false)
    private void frmc$suppressAxisMulPose(PoseStack pose, Quaternionf ignored) {
    }

    @Inject(method = "translateAndRotate", at = @At("RETURN"), remap = false)
    private void frmc$applyCombinedRotation(PoseStack pose, CallbackInfo callback) {
        if (frmc$rotationX != 0.0F || frmc$rotationY != 0.0F || frmc$rotationZ != 0.0F) {
            if (!frmc$hasCachedRotation || frmc$cachedRotationX != frmc$rotationX
                    || frmc$cachedRotationY != frmc$rotationY || frmc$cachedRotationZ != frmc$rotationZ) {
                frmc$combinedQuaternion.rotationZYX(frmc$rotationZ, frmc$rotationY, frmc$rotationX);
                frmc$cachedRotationX = frmc$rotationX;
                frmc$cachedRotationY = frmc$rotationY;
                frmc$cachedRotationZ = frmc$rotationZ;
                frmc$hasCachedRotation = true;
            }
            pose.mulPose(frmc$combinedQuaternion);
        }
    }

    @Unique
    private long frmc$transformStart;

    @Inject(method = "translateAndRotate", at = @At("HEAD"), remap = false)
    private void frmc$diagnosticTransformStart(PoseStack pose, CallbackInfo callback) {
        frmc$transformStart = EntityRenderDiagnostics.startTransform();
    }

    @Inject(method = "translateAndRotate", at = @At("RETURN"), remap = false)
    private void frmc$diagnosticTransformEnd(PoseStack pose, CallbackInfo callback) {
        EntityRenderDiagnostics.recordTransform(frmc$transformStart);
    }

    @Redirect(method = "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), remap = false)
    private void frmc$skipIdentityTransformScale(PoseStack pose, float x, float y, float z) {
        if (x != 1.0F || y != 1.0F || z != 1.0F) {
            pose.scale(x, y, z);
        }
    }

    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), remap = false)
    private void frmc$skipIdentityChildScale(PoseStack pose, float x, float y, float z) {
        if (x != 1.0F || y != 1.0F || z != 1.0F) {
            pose.scale(x, y, z);
        }
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$skipEmptyGeometry(PoseStack.Pose pose, VertexConsumer buffer,
                                         int packedLight, int packedOverlay,
                                         float red, float green, float blue, float alpha,
                                         CallbackInfo callback) {
        if (cubeList.isEmpty()) {
            EntityRenderDiagnostics.recordEmptyGeometry();
            callback.cancel();
        }
    }

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$renderWithAcceleratedPipeline(PoseStack.Pose pose, VertexConsumer buffer,
                                                     int packedLight, int packedOverlay,
                                                     float red, float green, float blue, float alpha,
                                                     CallbackInfo callback) {
        IAcceleratedVertexConsumer accelerated = VertexConsumerExtension.getAccelerated(buffer);
        if (!CoreFeature.isLoaded()
                || !CoreFeature.isRenderingLevel()
                || !AcceleratedEntityRenderingFeature.isEnabled()
                || !CitadelGeometryAccess.isAvailable()
                || !accelerated.isAccelerated()) {
            EntityRenderDiagnostics.recordCitadelFallback();
            return;
        }

        callback.cancel();
        accelerated.doRender(this, null, pose.pose(), pose.normal(), packedLight, packedOverlay,
                ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                        (int) (green * 255.0F), (int) (blue * 255.0F)));
    }

    @Override
    public void render(VertexConsumer buffer, Void context, Matrix4f transform, Matrix3f normal,
                       int light, int overlay, int color) {
        IAcceleratedVertexConsumer accelerated = VertexConsumerExtension.getAccelerated(buffer);
        if (frmc$meshes == null) {
            frmc$meshes = Maps.newIdentityHashMap();
            frmc$mergedMeshes = Maps.newHashMap();
        }
        IMesh mesh = frmc$meshes.get(accelerated);
        accelerated.beginTransform(transform, normal);
        try {
            if (mesh == null) {
                IMeshCollector collector = CoreFeature.createMeshCollector(accelerated);
                VertexConsumer meshBuilder = accelerated.decorate(collector);
                CitadelArRenderer.collectTabula(cubeList, meshBuilder, overlay);
                collector.flush();

                MeshData data = collector.getData();
                ByteBufferBuilder byteBuffer = collector.getBuffer();
                mesh = frmc$mergedMeshes.get(data);
                if (mesh != null) {
                    byteBuffer.close();
                } else {
                    mesh = AcceleratedEntityRenderingFeature.getMeshType().getBuilder().build(collector);
                }
                frmc$meshes.put(accelerated, mesh);
                frmc$mergedMeshes.put(data, mesh);
            }

            long diagnosticsStart = EntityRenderDiagnostics.start();
            mesh.write(accelerated, color, light, overlay);
            EntityRenderDiagnostics.recordArMeshWrite(diagnosticsStart);
        } finally {
            accelerated.endTransform();
        }
    }
}
