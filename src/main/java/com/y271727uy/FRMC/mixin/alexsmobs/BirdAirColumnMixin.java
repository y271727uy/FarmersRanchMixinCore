package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.entity.EntityPotoo;
import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import com.github.alexthe666.alexsmobs.entity.EntityToucan;
import com.y271727uy.FRMC.integration.alexsmobs.BirdGroundSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = {EntityBaldEagle.class, EntityBlueJay.class, EntityPotoo.class, EntitySeagull.class,
        EntityToucan.class}, remap = false)
public abstract class BirdAirColumnMixin {
    @ModifyVariable(method = {"isOverWaterOrVoid", "isOverLeaves"},
            at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 1)
    private BlockPos frmc$skipAirBelowBird(BlockPos start) {
        Entity bird = (Entity) (Object) this;
        int lowerLimit = -64;
        if (bird instanceof EntityToucan) {
            lowerLimit = -62;
        } else if (bird instanceof EntityBlueJay || bird instanceof EntityPotoo) {
            lowerLimit = -65;
        }
        return BirdGroundSearch.skipAir(bird.level(), start, lowerLimit);
    }
}
