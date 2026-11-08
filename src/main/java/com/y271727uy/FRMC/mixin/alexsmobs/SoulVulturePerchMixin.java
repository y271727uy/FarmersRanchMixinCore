package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntitySoulVulture;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.y271727uy.FRMC.integration.alexsmobs.BirdGroundSearch;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(EntitySoulVulture.class)
public abstract class SoulVulturePerchMixin {
    @ModifyExpressionValue(method = "findNewPerchPos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;offset(III)Lnet/minecraft/core/BlockPos;"), require = 1)
    private BlockPos frmc$skipAirAbovePerch(BlockPos candidate) {
        return BirdGroundSearch.skipAir(((EntitySoulVulture) (Object) this).level(), candidate, 1);
    }
}
