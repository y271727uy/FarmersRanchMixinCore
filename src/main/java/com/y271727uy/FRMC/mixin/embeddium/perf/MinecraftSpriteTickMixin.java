package com.y271727uy.FRMC.mixin.embeddium.perf;

import com.y271727uy.FRMC.integration.embeddium.perf.VisibleSpriteTicker;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 贴图动画在 Minecraft.tick 里推进，所以在 tick 开头标记一次可见区块段的动画贴图就够了。 */
@Mixin(Minecraft.class)
public abstract class MinecraftSpriteTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void frmc$markVisibleSprites(CallbackInfo ci) {
        if (!VisibleSpriteTicker.perTick()) {
            return;
        }
        SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
        if (renderer instanceof VisibleSpriteTicker ticker) {
            ticker.frmc$markVisibleSprites();
        }
    }
}
