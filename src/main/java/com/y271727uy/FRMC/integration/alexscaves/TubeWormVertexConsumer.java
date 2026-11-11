package com.y271727uy.FRMC.integration.alexscaves;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;

/** Keeps the pipe worm's camera-relative model on Citadel's regular vertex path. */
public final class TubeWormVertexConsumer extends VertexConsumerWrapper {
    public TubeWormVertexConsumer(VertexConsumer parent) {
        super(parent);
    }
}
