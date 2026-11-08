package com.y271727uy.FRMC.mixin.citadel.model;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;
import com.y271727uy.FRMC.integration.citadel.model.CitadelGeometryAccess;
import com.y271727uy.FRMC.integration.citadel.render.ar.CitadelMeshCache;
import com.y271727uy.FRMC.integration.citadel.model.CitadelSharedLocalTransformCache;
import com.y271727uy.FRMC.integration.citadel.model.CitadelTransformScratch;
import com.y271727uy.FRMC.integration.citadel.render.ar.ICitadelFastPart;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.FastColor.ARGB32;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BasicModelPart.class, remap = false, priority = 900)
public abstract class BasicModelPartMixin implements IAcceleratedRenderer<Void>, ICitadelFastPart {
    @Shadow @Final
    private ObjectList<BasicModelPart.ModelBox> cubeList;
    @Shadow @Final
    private ObjectList<BasicModelPart> childModels;

    @Unique
    private CitadelMeshCache frmc$meshCache;
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
    private boolean frmc$hasLocalTransform;
    @Unique
    private boolean frmc$localCombinedRotation;

    @Inject(method = "addChild", at = @At("RETURN"), remap = false)
    private void frmc$invalidateBasicChildren(BasicModelPart child, CallbackInfo callback) {
        frmc$childrenSnapshot = null;
        CitadelSkeletonLayoutCache.invalidateAll();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", at = @At("HEAD"), remap = false)
    private void frmc$diagnosticBoneRender(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                           CallbackInfo callback) {
        if (EntityRenderDiagnostics.enabled()) {
            EntityRenderDiagnostics.recordBoneRender();
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

    @Inject(method = "translateRotate", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$applyCombinedRotation(PoseStack pose, CallbackInfo callback) {
        frmc$transformStart = EntityRenderDiagnostics.startTransform();
        if (!Config.citadelCombinedRotationEnabled) {
            return;
        }

        BasicModelPart part = (BasicModelPart) (Object) this;
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
        EntityRenderDiagnostics.recordTransform(part, frmc$transformStart);
        callback.cancel();
    }

    @Inject(method = "translateRotate", at = @At("RETURN"), remap = false)
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
        if (frmc$meshCache == null) {
            frmc$meshCache = new CitadelMeshCache();
        }
        frmc$meshCache.render(cubeList, false, VertexConsumerExtension.getAccelerated(buffer),
                transform, normal, light, overlay, color);
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
    public int frmc$layoutFlags() {
        int flags = cubeList.isEmpty() ? 0 : ICitadelFastPart.FRMC_HAS_GEOMETRY;
        if (!childModels.isEmpty()) {
            flags |= ICitadelFastPart.FRMC_HAS_CHILDREN;
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
    public void frmc$applyLocalTransform(PoseStack pose) {
        BasicModelPart part = (BasicModelPart)(Object)this;
        int px = Float.floatToIntBits(part.rotationPointX);
        int py = Float.floatToIntBits(part.rotationPointY);
        int pz = Float.floatToIntBits(part.rotationPointZ);
        int rx = Float.floatToIntBits(part.rotateAngleX);
        int ry = Float.floatToIntBits(part.rotateAngleY);
        int rz = Float.floatToIntBits(part.rotateAngleZ);
        boolean combinedRotation = Config.citadelCombinedRotationEnabled;
        boolean changed = !frmc$hasLocalTransform || combinedRotation != frmc$localCombinedRotation
                || px != frmc$localPoseX || py != frmc$localPoseY || pz != frmc$localPoseZ
                || rx != frmc$localRotX || ry != frmc$localRotY || rz != frmc$localRotZ;
        if (changed) {
            int transformKind = combinedRotation ? 3 : 0;
            int identityScale = Float.floatToIntBits(1.0F);
            CitadelSharedLocalTransformCache.Entry shared = CitadelSharedLocalTransformCache.lookup(
                    transformKind, px, py, pz, rx, ry, rz, identityScale, identityScale, identityScale);
            if (shared != null) {
                frmc$localPose.set(shared.pose);
                frmc$localNormal.set(shared.normal);
                changed = false;
            } else {
                PoseStack scratch = CitadelTransformScratch.reset();
                part.translateRotate(scratch);
                frmc$localPose.set(scratch.last().pose());
                frmc$localNormal.set(scratch.last().normal());
                CitadelSharedLocalTransformCache.store(transformKind, px, py, pz, rx, ry, rz,
                        identityScale, identityScale, identityScale, frmc$localPose, frmc$localNormal);
            }
            frmc$localPoseX = px;
            frmc$localPoseY = py;
            frmc$localPoseZ = pz;
            frmc$localRotX = rx;
            frmc$localRotY = ry;
            frmc$localRotZ = rz;
            frmc$localCombinedRotation = combinedRotation;
            frmc$hasLocalTransform = true;
        }
        EntityRenderDiagnostics.recordCitadelTransformCache(!changed);
        pose.last().pose().mul(frmc$localPose);
        pose.last().normal().mul(frmc$localNormal);
    }

    @Override
    public void frmc$renderFast(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                                float red, float green, float blue, float alpha) {
        BasicModelPart part = (BasicModelPart)(Object)this;
        if (!part.showModel || (cubeList.isEmpty() && childModels.isEmpty())) {
            return;
        }
        EntityRenderDiagnostics.recordCitadelFastNode();
        pose.pushPose();
        try {
            ((ICitadelFastPart)(Object)this).frmc$applyLocalTransform(pose);
            frmc$renderNode(buffer, pose.last(), light, overlay, red, green, blue, alpha);
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
