package com.y271727uy.FRMC.mixin.constancy;

import com.y271727uy.FRMC.integration.constancy.ConstancyModernFixIntegration;
import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import net.minecraft.world.level.block.state.BlockState;
import com.google.common.collect.ImmutableSet;
import net.minecraftforge.client.event.ModelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands Constancy model wrapping to ModernFix's per-model bake integration.
 */
@Mixin(value = ModelWrappingHandler.class, remap = false)
public abstract class ModelWrappingHandlerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$registerModernFixIntegration(
            boolean wrapCtm,
            boolean wrapEmissive,
            ImmutableSet<BlockState> ctmAffectedStates,
            CallbackInfo ci) {
        ConstancyModernFixIntegration.register();
    }

    /**
     * Constancy's original listener scans the complete baked-model map. ModernFix
     * now invokes the same handler during each model bake, so the old pass would
     * be redundant and could wrap a model twice.
     */
    @Inject(method = "lambda$init$0", at = @At("HEAD"), cancellable = true)
    private static void frmc$disableFullModelScan(ModelEvent.ModifyBakingResult event, CallbackInfo ci) {
        if (ConstancyModernFixIntegration.isCallbackObserved()) {
            ci.cancel();
        }
    }
}
