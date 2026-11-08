package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityPotoo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.y271727uy.FRMC.integration.alexsmobs.BirdGroundSearch;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityPotoo$AIPerch")
public abstract class PotooPerchMixin {
    @Unique private EntityPotoo frmc$potoo;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void frmc$rememberPotoo(EntityPotoo potoo, CallbackInfo ci) {
        frmc$potoo = potoo;
    }

    @ModifyExpressionValue(method = "findPerch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;"), require = 1)
    private BlockPos frmc$skipAirAbovePerch(BlockPos candidate) {
        var level = frmc$potoo.level();
        if (level.isOutsideBuildHeight(candidate)) {
            return candidate;
        }
        BlockPos ground = BirdGroundSearch.skipAir(level, candidate, -64);
        // The original method checks whether the sampled position is loaded before descending.
        return level.isOutsideBuildHeight(ground) ? candidate : ground;
    }
}
