package com.y271727uy.FRMC.integration.untamedwilds.client.render;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.integration.oculus.OculusIntegration;
import java.util.WeakHashMap;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import untamedwilds.client.model.ModelBoar;
import untamedwilds.client.model.ModelBoarPiglet;
import untamedwilds.entity.mammal.EntityBoar;

public final class CitadelBoarLod {
    private static final float ENTER_PIXELS = 42.0F;
    private static final float EXIT_PIXELS = 50.0F;
    private static final WeakHashMap<EntityBoar, Boolean> REDUCED = new WeakHashMap<>();
    private static final Matrix4f VIEW_POSE = new Matrix4f();
    private static EntityBoar entity;

    private CitadelBoarLod() {
    }

    public static EntityBoar setEntity(EntityBoar current) {
        EntityBoar previous = entity;
        entity = current;
        return previous;
    }

    public static long omittedParts(Object model, BasicModelPart[] nodes, Matrix4f base) {
        EntityBoar boar = entity;
        if (boar == null || (OculusIntegration.IS_IRIS_INSTALLED && IrisApi.getInstance().isShaderPackInUse())) {
            return 0;
        }
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        if (projection.m23() != -1.0F || projection.m33() != 0.0F) {
            return 0;
        }
        RenderSystem.getModelViewMatrix().mul(base, VIEW_POSE);
        float depth = -VIEW_POSE.m32();
        // Reserve room around the model origin for the animated head and legs.
        float radius = Math.max(boar.getBbWidth(), boar.getBbHeight());
        int height = Minecraft.getInstance().getMainRenderTarget().viewHeight;
        float offAxis = Math.max(Math.abs(VIEW_POSE.m30()), Math.abs(VIEW_POSE.m31()));
        float pixels = depth > radius && height > 0 && radius > 0.0F
                ? height * Math.abs(projection.m11()) * radius / (depth - radius) * (1.0F + offAxis / depth)
                : Float.POSITIVE_INFINITY;
        boolean reduced = REDUCED.containsKey(boar);
        if (!Float.isFinite(pixels) || pixels >= (reduced ? EXIT_PIXELS : ENTER_PIXELS)) {
            if (reduced) {
                REDUCED.remove(boar);
            }
            return 0;
        }
        if (!reduced) {
            REDUCED.put(boar, Boolean.TRUE);
        }
        long omittedParts = 0;
        for (int i = 0; i < nodes.length; i++) {
            BasicModelPart node = nodes[i];
            if (model instanceof ModelBoar adult && (node == adult.eye_left || node == adult.eye_right
                    || node == adult.tusk_left || node == adult.tusk_left_1)
                    || model instanceof ModelBoarPiglet piglet && (node == piglet.eye_left || node == piglet.eye_right)) {
                omittedParts |= 1L << i;
            }
        }
        return omittedParts;
    }
}
