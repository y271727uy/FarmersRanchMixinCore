package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntitySunbird;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(EntitySunbird.class)
public abstract class SunbirdScorchSearchMixin {
    @Shadow(remap = false)
    private AABB getScorchArea() {
        throw new AssertionError();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", ordinal = 0, remap = false,
            target = "Lcom/github/alexthe666/alexsmobs/entity/EntitySunbird;getScorchingMobs()Ljava/util/List;"), require = 1)
    private List<LivingEntity> frmc$findFirstScorchTarget(EntitySunbird bird, Operation<List<LivingEntity>> original) {
        List<LivingEntity> first = new ArrayList<>(1);
        bird.level().getEntities(EntityTypeTest.forClass(LivingEntity.class), getScorchArea(), EntitySunbird.SCORCH_PRED,
                first, 1);
        return first;
    }
}
