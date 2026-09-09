package com.y271727uy.FRMC.mixin.manors_bounty.entity;

import com.y271727uy.FRMC.integration.manors_bounty.NestSearchHelper;
import net.mcreator.manors_bounty.entity.TurkeyEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(value = TurkeyEntity.FindNearestNestGoal.class, remap = false)
public abstract class TurkeyFindNearestNestGoalMixin {
    @Shadow
    @Final
    private TurkeyEntity turkey;

    /**
     * @author FRMC
     * @reason Search loaded block entities instead of checking up to 9,801 block states per attempt.
     */
    @Overwrite(remap = false)
    private BlockPos findNearestNest() {
        return NestSearchHelper.findNearestAvailableNest(this.turkey);
    }
}
