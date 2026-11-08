package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.client.render.ElectricCurrentRenderer;
import com.cloudmeow.delightoflight.entity.ElectricCurrentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = ElectricCurrentRenderer.class, remap = false)
public abstract class ElectricCurrentRendererMixin {
    @Inject(method = "render(Lcom/cloudmeow/delightoflight/entity/ElectricCurrentEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void frmc$skipTargetQueryOutsideRenderWindow(ElectricCurrentEntity entity, float yaw, float partialTick,
                                                        PoseStack pose, MultiBufferSource buffers, int light,
                                                        CallbackInfo ci) {
        if (entity.level().getGameTime() % 3L != 0L || Minecraft.getInstance().isPaused()) {
            ci.cancel();
        }
    }
}
