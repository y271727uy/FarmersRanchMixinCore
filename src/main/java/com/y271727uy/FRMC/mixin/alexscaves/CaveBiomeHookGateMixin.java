package com.y271727uy.FRMC.mixin.alexscaves;

import com.bawnorton.mixinsquared.TargetHandler;
import com.y271727uy.FRMC.integration.alexscaves.CaveBiomeHookGate;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips Alex's Caves' per-lookup cave biome hook in dimensions where no cave biome can generate. */
@Mixin(value = MultiNoiseBiomeSource.class, priority = 1500)
public abstract class CaveBiomeHookGateMixin {
    private static final String AC_MIXIN = "com.github.alexmodguy.alexscaves.mixin.MultiNoiseBiomeSourceMixin";

    @Dynamic("Alex's Caves cave biome replacement handler")
    @TargetHandler(mixin = AC_MIXIN, name = "citadel_getNoiseBiomeCoords")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$skipImpossibleCaveBiomes(int x, int y, int z, Climate.Sampler sampler,
                                               CallbackInfoReturnable<Holder<Biome>> original, CallbackInfo handler) {
        if (CaveBiomeHookGate.canSkip((MultiNoiseBiomeSource) (Object) this)) {
            handler.cancel();
        }
    }
}
