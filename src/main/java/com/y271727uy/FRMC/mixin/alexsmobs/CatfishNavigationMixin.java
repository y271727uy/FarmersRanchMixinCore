package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import com.y271727uy.FRMC.integration.computatio.ComputatioWaterNavigation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(EntityCatfish.class)
public abstract class CatfishNavigationMixin {
    @Inject(method = "createNavigation", at = @At("RETURN"), cancellable = true)
    private void frmc$useComputatioForShortSwims(Level level, CallbackInfoReturnable<PathNavigation> cir) {
        if (level instanceof ServerLevel serverLevel && cir.getReturnValue().getClass() == WaterBoundPathNavigation.class) {
            cir.setReturnValue(new ComputatioWaterNavigation((EntityCatfish) (Object) this, serverLevel));
        }
    }
}
