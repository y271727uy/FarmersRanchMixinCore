package com.y271727uy.FRMC.mixin.acceleratedrendering.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.FRMC.config.Config;
import com.y271727uy.FRMC.integration.untamedwilds.client.render.CitadelBoarLod;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import untamedwilds.client.render.RendererBoar;
import untamedwilds.entity.mammal.EntityBoar;

@Pseudo
@Mixin(value = RendererBoar.class, remap = false)
public abstract class RendererBoarLodMixin {
    @WrapMethod(method = "render(Luntamedwilds/entity/mammal/EntityBoar;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", require = 0)
    private void frmc$selectBoarDetail(EntityBoar entity, float yaw, float partialTick, PoseStack poses,
                                      MultiBufferSource buffers, int light, Operation<Void> original) {
        EntityBoar previous = CitadelBoarLod.setEntity(Config.citadelBoarLodEnabled ? entity : null);
        try {
            original.call(entity, yaw, partialTick, poses, buffers, light);
        } finally {
            CitadelBoarLod.setEntity(previous);
        }
    }
}
