package com.y271727uy.FRMC.mixin.citadel.model;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelFastPart;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.model.CitadelGeometryAccess;
import com.y271727uy.FRMC.integration.citadel.model.CitadelLayoutOwner;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelGpuPoseBatch;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelMeshCache;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelGpuPart;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSharedLocalTransformCache;
import com.y271727uy.FRMC.integration.citadel.model.CitadelTransformScratch;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAccelerationHolder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import it.unimi.dsi.fastutil.objects.ObjectList;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AdvancedModelBox.class, remap = false, priority = 900)
public abstract class AdvancedModelBoxMixin implements IAcceleratedRenderer<Void>, ICitadelFastPart,
        ICitadelGpuPart, CitadelLayoutOwner {
    @Shadow
    public ObjectList<?> cubeList;

    @Unique
    private CitadelMeshCache frmc$meshCache;
    @Unique
    private CitadelSkeletonLayoutCache.Layout frmc$layout;
    @Unique
    private long frmc$layoutRevision;
    @Unique
    private BasicModelPart[] frmc$childrenSnapshot;
    @Unique
    private final Matrix4f frmc$localPose = new Matrix4f();
    @Unique
    private final Matrix3f frmc$localNormal = new Matrix3f();
    @Unique
    private int frmc$localPoseX;
    @Unique
    private int frmc$localPoseY;
    @Unique
    private int frmc$localPoseZ;
    @Unique
    private int frmc$localRotX;
    @Unique
    private int frmc$localRotY;
    @Unique
    private int frmc$localRotZ;
    @Unique
    private int frmc$localScaleX;
    @Unique
    private int frmc$localScaleY;
    @Unique
    private int frmc$localScaleZ;
    @Unique
    private boolean frmc$localCombinedRotation;
    @Unique
    private boolean frmc$hasLocalTransform;
    @Shadow
    public float scaleX;
    @Shadow
    public float scaleY;
    @Shadow
    public float scaleZ;
    @Shadow
    public boolean scaleChildren;
    @Shadow
    public ObjectList<BasicModelPart> childModels;

    @Inject(method = "addChild", at = @At("RETURN"), remap = false)
    private void frmc$invalidateChildren(BasicModelPart child, CallbackInfo callback) {
        frmc$childrenSnapshot = null;
        frmc$layout = null;
        CitadelSkeletonLayoutCache.invalidateAll();
    }

    @Override
    public CitadelSkeletonLayoutCache.Layout frmc$citadelLayout(long revision) {
        return frmc$layoutRevision == revision ? frmc$layout : null;
    }

    @Override
    public void frmc$setCitadelLayout(long revision, CitadelSkeletonLayoutCache.Layout layout) {
        frmc$layoutRevision = revision;
        frmc$layout = layout;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$renderFastRoot(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                     float red, float green, float blue, float alpha,
                                     CallbackInfo callback) {
        boolean diagnostics = EntityRenderDiagnostics.enabled();
        boolean gpu = Config.citadelGpuPoseRenderingEnabled && Config.citadelCombinedRotationEnabled;
        if (!diagnostics && !gpu && !Config.citadelFastTraversalEnabled) {
            return;
        }
        AdvancedModelBox current = (AdvancedModelBox)(Object)this;
        boolean root = current.getParent() == null;
        if (diagnostics) {
            if (root) {
                EntityRenderDiagnostics.beginCitadelRootRender(
                        VertexConsumerExtension.getAccelerated(buffer).isAccelerated());
                EntityRenderDiagnostics.recordSoaShadow(current);
                EntityRenderDiagnostics.logBigCatBoneTree(current);
            }
            EntityRenderDiagnostics.recordBoneRender();
            if (scaleX == 1.0F && scaleY == 1.0F && scaleZ == 1.0F) {
                EntityRenderDiagnostics.recordUnitScale();
            }
        }

        if (!root || (!gpu && !Config.citadelFastTraversalEnabled)) {
            return;
        }
        IAcceleratedVertexConsumer rootConsumer = VertexConsumerExtension.getAccelerated(buffer);
        if (!CoreFeature.isLoaded()
                || !CoreFeature.isRenderingLevel()
                || !AcceleratedEntityRenderingFeature.isEnabled()
                || !CitadelGeometryAccess.isAvailable()
                || !rootConsumer.isAccelerated()) {
            return;
        }

        if (gpu) {
            AcceleratedBufferBuilder builder = null;
            if (rootConsumer.getClass() == AcceleratedBufferBuilder.class) {
                builder = (AcceleratedBufferBuilder) rootConsumer;
            } else if (buffer.getClass() == BufferBuilder.class && buffer instanceof IAccelerationHolder holder) {
                builder = holder.getAccelerated();
            }
            if (builder != null && builder.getClass() == AcceleratedBufferBuilder.class
                    && CitadelGpuPoseBatch.render(current, pose.last(), builder, light, overlay,
                            ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                                    (int) (green * 255.0F), (int) (blue * 255.0F)))) {
                if (diagnostics) {
                    EntityRenderDiagnostics.finishCitadelRootRender();
                }
                callback.cancel();
                return;
            }
        }
        if (!Config.citadelFastTraversalEnabled) {
            return;
        }
        EntityRenderDiagnostics.recordCitadelFastRoot();
        ((ICitadelFastPart)(Object)this).frmc$renderFast(
                pose, buffer, light, overlay, red, green, blue, alpha);
        EntityRenderDiagnostics.finishCitadelFastRoot();
        if (diagnostics) {
            EntityRenderDiagnostics.finishCitadelRootRender();
        }
        callback.cancel();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("RETURN"), remap = false)
    private void frmc$finishRootRender(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                       float red, float green, float blue, float alpha,
                                       CallbackInfo callback) {
        if (EntityRenderDiagnostics.enabled()
                && ((AdvancedModelBox)(Object)this).getParent() == null) {
            EntityRenderDiagnostics.finishCitadelRootRender();
        }
    }

    @Unique
    private long frmc$transformStart;
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

    @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$applyCombinedRotation(PoseStack pose, CallbackInfo callback) {
        frmc$transformStart = EntityRenderDiagnostics.startTransform();
        if (!Config.citadelCombinedRotationEnabled) {
            return;
        }

        AdvancedModelBox part = (AdvancedModelBox) (Object) this;
        pose.translate((double) (part.rotationPointX / 16.0F), (double) (part.rotationPointY / 16.0F),
                (double) (part.rotationPointZ / 16.0F));
        float x = part.rotateAngleX;
        float y = part.rotateAngleY;
        float z = part.rotateAngleZ;
        if (x != 0.0F || y != 0.0F || z != 0.0F) {
            if (!frmc$hasCachedRotation || frmc$cachedRotationX != x
                    || frmc$cachedRotationY != y || frmc$cachedRotationZ != z) {
                frmc$combinedQuaternion.rotationZYX(z, y, x);
                frmc$cachedRotationX = x;
                frmc$cachedRotationY = y;
                frmc$cachedRotationZ = z;
                frmc$hasCachedRotation = true;
            }
            pose.mulPose(frmc$combinedQuaternion);
        }
        if (scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F) {
            pose.scale(scaleX, scaleY, scaleZ);
        }
        EntityRenderDiagnostics.recordTransform(part, frmc$transformStart);
        callback.cancel();
    }

    @Inject(method = "translateAndRotate", at = @At("RETURN"), remap = false)
    private void frmc$diagnosticTransformEnd(PoseStack pose, CallbackInfo callback) {
        EntityRenderDiagnostics.recordTransform((BasicModelPart)(Object)this, frmc$transformStart);
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
        frmc$citadelMeshCache().render(cubeList, true, VertexConsumerExtension.getAccelerated(buffer),
                transform, normal, light, overlay, color);
    }

    @Override
    public CitadelMeshCache frmc$citadelMeshCache() {
        if (frmc$meshCache == null) {
            frmc$meshCache = new CitadelMeshCache();
        }
        return frmc$meshCache;
    }

    @Override
    public void frmc$renderNode(VertexConsumer buffer, PoseStack.Pose pose, int light, int overlay,
                                float red, float green, float blue, float alpha) {
        if (cubeList.isEmpty()) {
            EntityRenderDiagnostics.recordEmptyGeometry();
            return;
        }
        IAcceleratedVertexConsumer accelerated = VertexConsumerExtension.getAccelerated(buffer);
        accelerated.doRender(this, null, pose.pose(), pose.normal(), light, overlay,
                ARGB32.color((int) (alpha * 255.0F), (int) (red * 255.0F),
                        (int) (green * 255.0F), (int) (blue * 255.0F)));
    }

    @Override
    public void frmc$applyLocalTransform(PoseStack pose) {
        AdvancedModelBox part = (AdvancedModelBox)(Object)this;
        int px = Float.floatToIntBits(part.rotationPointX);
        int py = Float.floatToIntBits(part.rotationPointY);
        int pz = Float.floatToIntBits(part.rotationPointZ);
        int rx = Float.floatToIntBits(part.rotateAngleX);
        int ry = Float.floatToIntBits(part.rotateAngleY);
        int rz = Float.floatToIntBits(part.rotateAngleZ);
        int sx = Float.floatToIntBits(scaleX);
        int sy = Float.floatToIntBits(scaleY);
        int sz = Float.floatToIntBits(scaleZ);
        boolean combinedRotation = Config.citadelCombinedRotationEnabled;
        boolean changed = !frmc$hasLocalTransform || combinedRotation != frmc$localCombinedRotation
                || px != frmc$localPoseX || py != frmc$localPoseY || pz != frmc$localPoseZ
                || rx != frmc$localRotX || ry != frmc$localRotY || rz != frmc$localRotZ
                || sx != frmc$localScaleX || sy != frmc$localScaleY || sz != frmc$localScaleZ;
        if (changed) {
            int transformKind = combinedRotation ? 2 : 1;
            CitadelSharedLocalTransformCache.Entry shared = CitadelSharedLocalTransformCache.lookup(
                    transformKind, px, py, pz, rx, ry, rz, sx, sy, sz);
            if (shared != null) {
                frmc$localPose.set(shared.pose);
                frmc$localNormal.set(shared.normal);
                changed = false;
            } else {
                PoseStack scratch = CitadelTransformScratch.reset();
                part.translateAndRotate(scratch);
                frmc$localPose.set(scratch.last().pose());
                frmc$localNormal.set(scratch.last().normal());
                CitadelSharedLocalTransformCache.store(transformKind, px, py, pz, rx, ry, rz, sx, sy, sz,
                        frmc$localPose, frmc$localNormal);
            }
            frmc$localPoseX = px;
            frmc$localPoseY = py;
            frmc$localPoseZ = pz;
            frmc$localRotX = rx;
            frmc$localRotY = ry;
            frmc$localRotZ = rz;
            frmc$localScaleX = sx;
            frmc$localScaleY = sy;
            frmc$localScaleZ = sz;
            frmc$localCombinedRotation = combinedRotation;
            frmc$hasLocalTransform = true;
        }
        EntityRenderDiagnostics.recordCitadelTransformCache(!changed);
        pose.last().pose().mul(frmc$localPose);
        pose.last().normal().mul(frmc$localNormal);
    }

    @Override
    public int frmc$layoutFlags() {
        int flags = cubeList.isEmpty() ? 0 : ICitadelFastPart.FRMC_HAS_GEOMETRY;
        if (!childModels.isEmpty()) {
            flags |= ICitadelFastPart.FRMC_HAS_CHILDREN;
        }
        flags |= ICitadelFastPart.FRMC_ADVANCED;
        if (scaleChildren) {
            flags |= ICitadelFastPart.FRMC_SCALE_CHILDREN;
        }
        return flags;
    }

    @Override
    public BasicModelPart[] frmc$childrenFast() {
        if (frmc$childrenSnapshot == null) {
            frmc$childrenSnapshot = childModels.toArray(new BasicModelPart[0]);
        }
        return frmc$childrenSnapshot;
    }

    @Override
    public void frmc$renderFast(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                float red, float green, float blue, float alpha) {
        AdvancedModelBox part = (AdvancedModelBox)(Object)this;
        if (!part.showModel || (cubeList.isEmpty() && childModels.isEmpty())) {
            return;
        }
        EntityRenderDiagnostics.recordCitadelFastNode();
        pose.pushPose();
        try {
            ((ICitadelFastPart)(Object)this).frmc$applyLocalTransform(pose);
            frmc$renderNode(buffer, pose.last(), light, overlay, red, green, blue, alpha);
            if (!part.scaleChildren && (scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F)) {
                pose.scale(1.0F / Math.max(scaleX, 1.0E-4F),
                        1.0F / Math.max(scaleY, 1.0E-4F),
                        1.0F / Math.max(scaleZ, 1.0E-4F));
            }
            BasicModelPart[] children = ((ICitadelFastPart)(Object)this).frmc$childrenFast();
            for (int i = 0, size = children.length; i < size; i++) {
                BasicModelPart child = children[i];
                if (child instanceof ICitadelFastPart fastPart) {
                    fastPart.frmc$renderFast(pose, buffer, light, overlay, red, green, blue, alpha);
                } else {
                    child.render(pose, buffer, light, overlay, red, green, blue, alpha);
                }
            }
        } finally {
            pose.popPose();
        }
    }
}
