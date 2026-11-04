package com.y271727uy.FRMC.mixin.untamed_wilds.client.model;

import com.y271727uy.FRMC.config.UntamedWildsAnimationConfig;
import com.y271727uy.FRMC.integration.diagnostics.EntityRenderDiagnostics;
import com.y271727uy.FRMC.integration.untamedwilds.UntamedWildsBobCache;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import untamedwilds.client.model.ModelBigCat;
import untamedwilds.entity.mammal.EntityBigCat;

/** Reuses identical bob calculations within one big-cat animation pass. */
@Pseudo
@Mixin(value = ModelBigCat.class, remap = false)
public abstract class ModelBigCatMixin {
    @Unique
    private long frmc$diagnosticStart;

    @Unique
    private final UntamedWildsBobCache frmc$bobCache = new UntamedWildsBobCache();

    @Inject(method = "setupAnim", at = @At("HEAD"), remap = false)
    private void frmc$resetBobCache(EntityBigCat entity, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float netHeadYaw, float headPitch,
                                     CallbackInfo callback) {
        frmc$diagnosticStart = EntityRenderDiagnostics.start();
        frmc$bobCache.valid = false;
    }

    @Inject(method = "setupAnim", at = @At("RETURN"), remap = false)
    private void frmc$recordDiagnostics(EntityBigCat entity, float limbSwing, float limbSwingAmount,
                                        float ageInTicks, float netHeadYaw, float headPitch,
                                        CallbackInfo callback) {
        EntityRenderDiagnostics.recordBigCat(frmc$diagnosticStart);
    }

    @Redirect(
            method = "setupAnim",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/citadel/client/model/AdvancedModelBox;bob(FFZFF)V",
                    remap = false),
            remap = false)
    private void frmc$cachedBob(AdvancedModelBox part, float speed, float degree, boolean bounce,
                                float offset, float weight) {
        if (!UntamedWildsAnimationConfig.isEnabled()) {
            part.bob(speed, degree, bounce, offset, weight);
            return;
        }
        UntamedWildsBobCache cache = frmc$bobCache;
        float movementScale = part.getModel().getMovementScale();
        if (!cache.valid || cache.speed != speed || cache.degree != degree || cache.bounce != bounce
                || cache.offset != offset || cache.weight != weight || cache.movementScale != movementScale) {
            float scaledSpeed = speed * movementScale;
            float scaledDegree = degree * movementScale;
            float result = (float) (Math.sin((double) (offset * scaledSpeed)) * (double) weight
                    * (double) scaledDegree - (double) weight * (double) scaledDegree);
            if (bounce) {
                result = (float) -(Math.abs(Math.sin((double) (offset * scaledSpeed))
                        * (double) weight * (double) scaledDegree));
            }
            cache.speed = speed;
            cache.degree = degree;
            cache.bounce = bounce;
            cache.offset = offset;
            cache.weight = weight;
            cache.movementScale = movementScale;
            cache.result = result;
            cache.valid = true;
        }
        part.rotationPointY += cache.result;
    }
}
