package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearSirenBlockEntity;
import com.github.alexmodguy.alexscaves.server.block.poi.ACPOIRegistry;
import com.y271727uy.FRMC.integration.alexscaves.SphericalPoiSearch;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NuclearSirenBlockEntity.class, remap = false)
public abstract class NuclearSirenPoiMixin {
    @Shadow(remap = false) protected abstract boolean isCriticalFurnace(BlockPos pos);

    @Inject(method = "getNearbyCriticalFurnaces", at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$findCriticalFurnace(ServerLevel level, int range, CallbackInfoReturnable<Stream<BlockPos>> cir) {
        if (range != 128 || !SphericalPoiSearch.canSearch(level)) {
            return;
        }
        var center = ((BlockEntity) (Object) this).getBlockPos();
        var match = SphericalPoiSearch.visit(level, center, range, ACPOIRegistry.NUCLEAR_FURNACE.getKey(),
                this::isCriticalFurnace);
        cir.setReturnValue(Stream.ofNullable(match));
    }
}
