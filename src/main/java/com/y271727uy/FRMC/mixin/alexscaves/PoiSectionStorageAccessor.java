package com.y271727uy.FRMC.mixin.alexscaves;

import java.util.Optional;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionStorage.class)
public interface PoiSectionStorageAccessor<R> {
    @Invoker("getOrLoad")
    Optional<R> frmc$getOrLoadPoiSection(long section);
}
