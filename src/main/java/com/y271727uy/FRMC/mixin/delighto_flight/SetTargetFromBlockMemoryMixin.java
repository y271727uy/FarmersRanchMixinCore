package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.entity.ai.SetTargetFromBlockMemory;
import com.mojang.datafixers.kinds.IdF;
import com.y271727uy.FRMC.integration.delighto_flight.AllayRelaySearch;
import com.y271727uy.FRMC.integration.delighto_flight.ChefWalkTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = SetTargetFromBlockMemory.class, remap = false)
public abstract class SetTargetFromBlockMemoryMixin {
    @Unique
    private static final String FRMC_WORK_TRIGGER = "lambda$create$0("
            + "Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;"
            + "Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;"
            + "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;"
            + "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;II"
            + "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;FI"
            + "Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/allay/Allay;J)Z";

    @Inject(method = FRMC_WORK_TRIGGER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/util/DefaultRandomPos;getPosTowards(Lnet/minecraft/world/entity/PathfinderMob;IILnet/minecraft/world/phys/Vec3;D)Lnet/minecraft/world/phys/Vec3;",
            remap = true), cancellable = true, require = 0)
    private static void frmc$limitRelaySearch(MemoryModuleType<GlobalPos> workMemory,
                                              BehaviorBuilder.Instance<Allay> builder,
                                              MemoryAccessor<IdF.Mu, GlobalPos> work,
                                              MemoryAccessor<?, Long> cantReach, int timeout,
                                              int maxDistance, MemoryAccessor<?, WalkTarget> walk,
                                              float speed, int closeEnough, ServerLevel level,
                                              Allay allay, long gameTime,
                                              CallbackInfoReturnable<Boolean> cir) {
        // Only the far-target branch reaches this call. DF has already validated the work memory.
        GlobalPos target = builder.get(work);
        AllayRelaySearch search = AllayRelaySearch.forAllay(allay);
        Vec3 destination = Vec3.atBottomCenterOf(target.pos());
        while (search.trySample(gameTime, workMemory, target)) {
            Vec3 waypoint = DefaultRandomPos.getPosTowards(allay, 15, 7, destination, (float) (Math.PI / 2));
            if (search.isExhausted()) {
                search.finish();
                SetTargetFromBlockMemory.releasePoi(workMemory, allay);
                work.erase();
                cantReach.set(gameTime);
                break;
            }
            if (waypoint != null && BlockPos.containing(waypoint).distManhattan(allay.blockPosition()) <= maxDistance) {
                search.finish();
                walk.set(new ChefWalkTarget(new WalkTarget(waypoint, speed, closeEnough)));
                break;
            }
        }

        // Yield to the next tick with the work memory intact and no walk target.
        cir.setReturnValue(true);
    }

    @ModifyArg(method = FRMC_WORK_TRIGGER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;set(Ljava/lang/Object;)V",
            remap = true), index = 0, require = 0)
    private static Object frmc$markWorkTarget(Object value) {
        // This trigger also writes timestamps to a different memory through the same erased setter.
        if (value instanceof WalkTarget target) {
            return new ChefWalkTarget(target);
        }
        return value;
    }
}
