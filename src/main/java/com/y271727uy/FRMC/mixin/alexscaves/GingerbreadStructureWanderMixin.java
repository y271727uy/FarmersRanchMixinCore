package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.MobWanderThroughStructureGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobWanderThroughStructureGoal.class)
public abstract class GingerbreadStructureWanderMixin extends RandomStrollGoal {
    @Shadow(remap = false) private int errorCooldown;
    @Unique private BlockPos frmc$lastEmptyOrigin;
    @Unique private long frmc$nextStructureSearch;

    protected GingerbreadStructureWanderMixin(PathfinderMob mob, double speed) {
        super(mob, speed);
    }

    @WrapMethod(method = "getPosition", require = 1)
    private Vec3 frmc$limitEmptyStructureSearch(Operation<Vec3> original) {
        if (errorCooldown > 0) {
            return null;
        }
        if (!(mob instanceof GingerbreadManEntity) || !(mob.level() instanceof ServerLevel level)) {
            return original.call();
        }
        BlockPos origin = mob.blockPosition();
        long now = level.getGameTime();
        if (frmc$lastEmptyOrigin != null && now < frmc$nextStructureSearch
                && origin.distSqr(frmc$lastEmptyOrigin) < 16.0D) {
            return null;
        }
        Vec3 position = original.call();
        frmc$lastEmptyOrigin = position == null ? origin : null;
        if (position == null) {
            frmc$nextStructureSearch = now + 40L;
        }
        return position;
    }

    @ModifyExpressionValue(method = "getPosition", at = @At(value = "FIELD", remap = false, opcode = Opcodes.GETFIELD,
            target = "Lcom/github/alexmodguy/alexscaves/server/entity/ai/MobWanderThroughStructureGoal;errorCooldown:I"),
            require = 1)
    private int frmc$requireValidStructureAfterSearchError(int cooldown) {
        // A failed lookup sets this field and returns null. Do not enter the structure-piece loop.
        return 0;
    }
}
