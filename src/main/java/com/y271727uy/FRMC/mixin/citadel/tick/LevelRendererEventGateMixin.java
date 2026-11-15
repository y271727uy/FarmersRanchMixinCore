package com.y271727uy.FRMC.mixin.citadel.tick;

import com.bawnorton.mixinsquared.TargetHandler;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelRendererEvents;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Citadel 用一个 @Redirect 替换 renderLevel 里的 Entity.getTeamColor() 调用，并在其中 post
 * EventGetOutlineColor。合并后它只是一个返回 int 的普通方法，所以同样用 MixinSquared 在其头部注入：
 * 没人监听时直接返回原版的 getTeamColor()，与 Citadel 无监听器时的结果一致。
 */
@Pseudo
@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class LevelRendererEventGateMixin {
    @Dynamic("Citadel LevelRenderer team color redirect handler")
    @TargetHandler(
            mixin = "com.github.alexthe666.citadel.mixin.client.LevelRendererMixin",
            name = "citadel_getTeamColor")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$gateOutlineColor(Entity entity, CallbackInfoReturnable<Integer> citadel) {
        // The Redirect handler takes only the entity, so unlike the other gates there is no original callback.
        if (!CitadelRendererEvents.wantsGetOutlineColor()) {
            citadel.setReturnValue(entity.getTeamColor());
        }
    }
}
