package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.client.particle.MagneticCavesAmbientParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagneticCavesAmbientParticle.class, remap = false)
public abstract class MagneticAmbientScanMixin {
    @Inject(method = "getStartPosition", at = @At("HEAD"), cancellable = true, require = 1)
    private static void frmc$scanWithMutablePosition(ClientLevel level, boolean goUp, double x, double y, double z,
            CallbackInfoReturnable<Vec3> cir) {
        var pos = new BlockPos.MutableBlockPos().set(x, y, z);
        if (goUp) {
            int limit = level.getMaxBuildHeight();
            while (pos.getY() < limit && level.getBlockState(pos).isAir()) {
                pos.move(0, 1, 0);
            }
        } else {
            int limit = level.getMinBuildHeight();
            while (pos.getY() > limit && level.getBlockState(pos).isAir()) {
                pos.move(0, -1, 0);
            }
        }
        cir.setReturnValue(Vec3.atBottomCenterOf(pos).add(0, goUp ? 0 : 1, 0));
    }
}
