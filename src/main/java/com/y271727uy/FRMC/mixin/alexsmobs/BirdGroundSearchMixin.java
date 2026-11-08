package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.entity.EntityBlueJay;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.entity.EntityPotoo;
import com.github.alexthe666.alexsmobs.entity.EntitySeagull;
import com.github.alexthe666.alexsmobs.entity.EntityToucan;
import com.y271727uy.FRMC.integration.alexsmobs.BirdGroundSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = {EntityBaldEagle.class, EntityBlueJay.class, EntityCrow.class,
        EntityPotoo.class, EntitySeagull.class, EntityToucan.class}, remap = false)
public abstract class BirdGroundSearchMixin {
    @Inject(method = {"getCrowGround", "getBlueJayGround", "getToucanGround", "getSeagullGround"},
            at = @At("HEAD"), cancellable = true)
    private void frmc$scanGroundBySection(BlockPos column, CallbackInfoReturnable<BlockPos> cir) {
        Entity bird = (Entity) (Object) this;
        BlockPos ground = BirdGroundSearch.find(bird, column, !(bird instanceof EntityCrow),
                !(bird instanceof EntityBaldEagle));
        if (ground != null) {
            cir.setReturnValue(ground);
        }
    }
}
