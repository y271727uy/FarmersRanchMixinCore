package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.VentSmokeParticle;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VentSmokeParticle.class)
public abstract class VentSmokeLightMixin extends TextureSheetParticle {
    @Shadow(remap = false) @Final private boolean fullbright;
    @Unique private long frmc$lightTick = Long.MIN_VALUE;
    @Unique private long frmc$lightPosition;
    @Unique private int frmc$packedLight;

    protected VentSmokeLightMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @WrapMethod(method = "getLightColor", require = 1)
    private int frmc$reuseVentSmokeLight(float partialTick, Operation<Integer> original) {
        if (fullbright) {
            return 240;
        }

        // Particle.getLightColor samples the current block, independent of partialTick.
        long tick = level.getGameTime();
        long position = BlockPos.asLong(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        if (tick != frmc$lightTick || position != frmc$lightPosition) {
            frmc$packedLight = original.call(partialTick);
            frmc$lightPosition = position;
            frmc$lightTick = tick;
        }
        return frmc$packedLight;
    }
}
