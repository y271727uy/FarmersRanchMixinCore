package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ai.GingerbreadManStoreStolenItemsGoal;
import com.github.alexmodguy.alexscaves.server.entity.ai.LicowitchUseCrucibleGoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GingerbreadManStoreStolenItemsGoal.class, LicowitchUseCrucibleGoal.class})
public abstract class CandyWorkplaceGoalMixin {
    @Unique private PathNavigation frmc$failedNavigator;
    @Unique private BlockPos frmc$failedDestination;
    @Unique private int frmc$pathRetryDelay;

    @WrapOperation(method = "canUse", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/stream/Stream;sorted(Ljava/util/Comparator;)Ljava/util/stream/Stream;"), require = 1)
    private Stream<BlockPos> frmc$selectNearestWorkplace(Stream<BlockPos> candidates, Comparator<BlockPos> distance,
            Operation<Stream<BlockPos>> original) {
        try (candidates) {
            return Stream.ofNullable(candidates.reduce((first, second) ->
                    distance.compare(first, second) <= 0 ? first : second).orElse(null));
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(DDDD)Z"), require = 1)
    private boolean frmc$limitFailedWorkplacePaths(PathNavigation navigation, double x, double y, double z, double speed,
            Operation<Boolean> original) {
        if (frmc$pathRetryDelay > 0 && navigation == frmc$failedNavigator && navigation.isDone()
                && frmc$failedDestination.equals(BlockPos.containing(x, y, z))) {
            frmc$pathRetryDelay--;
            return false;
        }
        boolean started = original.call(navigation, x, y, z, speed);
        if (started) {
            frmc$clearWorkplaceRetry();
        } else {
            frmc$failedNavigator = navigation;
            frmc$failedDestination = BlockPos.containing(x, y, z);
            frmc$pathRetryDelay = 3;
        }
        return started;
    }

    @Inject(method = "stop", at = @At("HEAD"), require = 1)
    private void frmc$resetWorkplaceRetry(CallbackInfo ci) {
        frmc$clearWorkplaceRetry();
    }

    @Unique
    private void frmc$clearWorkplaceRetry() {
        frmc$pathRetryDelay = 0;
        frmc$failedNavigator = null;
        frmc$failedDestination = null;
    }
}
