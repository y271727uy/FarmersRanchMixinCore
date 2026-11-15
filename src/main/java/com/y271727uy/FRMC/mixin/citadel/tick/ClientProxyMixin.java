package com.y271727uy.FRMC.mixin.citadel.tick;

import com.github.alexthe666.citadel.ClientProxy;
import com.github.alexthe666.citadel.client.tick.ClientTickRateTracker;
import com.y271727uy.FRMC.integration.citadel.tick.HoverAnimationState;
import com.y271727uy.FRMC.integration.citadel.tick.PatreonIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Citadel 客户端代理：每实体 tick 检查短路、悬停动画去装箱、Patreon 跟随者渲染提前剔除。 */
@Pseudo
@Mixin(value = ClientProxy.class, remap = false)
public abstract class ClientProxyMixin {
    @Shadow
    private ItemStack lastHoveredItem;

    @Inject(method = "canEntityTickClient", at = @At("HEAD"), cancellable = true)
    private void frmc$fastEntityTickCheck(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ClientTickRateTracker tracker = ClientTickRateTracker.getForClient(Minecraft.getInstance());
        if (tracker.tickRateModifierList.isEmpty() && tracker.specialTickRateEntities.isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tickMouseOverAnimations", at = @At("HEAD"), cancellable = true)
    private void frmc$tickMouseOverAnimations(CallbackInfo ci) {
        ci.cancel();
        HoverAnimationState.tick(this.lastHoveredItem);
        this.lastHoveredItem = null;
    }

    @Inject(method = "getMouseOverProgress", at = @At("HEAD"), cancellable = true)
    private void frmc$getMouseOverProgress(ItemStack itemStack, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(HoverAnimationState.progress(itemStack));
    }

    @Inject(method = "playerRender", at = @At("HEAD"), cancellable = true)
    private void frmc$playerRender(RenderPlayerEvent.Post event, CallbackInfo ci) {
        if (ClientProxy.hideFollower) {
            ci.cancel();
            return;
        }
        Player player = event.getEntity();
        if (!player.isModelPartShown(PlayerModelPart.CAPE) || player.isSpectator()) {
            ci.cancel();
            return;
        }
        if (PatreonIndex.isEmpty() || !PatreonIndex.isPatreon(player.getGameProfile().getName())) {
            ci.cancel();
        }
    }
}
