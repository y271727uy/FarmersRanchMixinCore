package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.poi.ACPOIRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.NucleeperEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.alexscaves.SphericalPoiSearch;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({NucleeperEntity.class, NuclearBombEntity.class})
public abstract class NuclearSirenActivationMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"), require = 1, allow = 1)
    private void frmc$activateSirensInSphere(Stream<BlockPos> sirens, Consumer<BlockPos> activate, Operation<Void> original) {
        var entity = (Entity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level) || !SphericalPoiSearch.canSearch(level)) {
            original.call(sirens, activate);
            return;
        }
        try (sirens) {
            SphericalPoiSearch.visit(level, entity.blockPosition(), 256, ACPOIRegistry.NUCLEAR_SIREN.getKey(), pos -> {
                activate.accept(pos);
                return false;
            });
        }
    }
}
