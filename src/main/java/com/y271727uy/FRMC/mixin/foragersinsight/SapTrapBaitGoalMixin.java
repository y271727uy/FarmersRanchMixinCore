package com.y271727uy.FRMC.mixin.foragersinsight;

import com.tiomadre.foragersinsight.core.other.toolevents.SapTrapBaitGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Preserves the original average search rate without retrying a 33x5x33 scan every tick.
 *
 * <p>Forager's Insight is distributed with SRG method names in the production
 * Forge jar ({@code m_8036_}), while some development environments expose the
 * named alias ({@code canUse}). Target both names so the optimization remains
 * valid across the two runtime naming forms.</p>
 */
@Mixin(value = SapTrapBaitGoal.class, remap = false)
public abstract class SapTrapBaitGoalMixin {
    @Shadow private PathfinderMob mob;
    @Shadow private BlockPos targetPos;
    @Shadow abstract BlockPos findBaitedTrap();
    @Unique private long frmc$nextSearchTick;

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$optimizedCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob.getTarget() != null) {
            cir.setReturnValue(false);
            return;
        }
        long now = this.mob.level().getGameTime();
        if (now < this.frmc$nextSearchTick) {
            cir.setReturnValue(false);
            return;
        }
        this.frmc$nextSearchTick = now + 20L;
        this.targetPos = this.findBaitedTrap();
        cir.setReturnValue(this.targetPos != null);
    }
}
