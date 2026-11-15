package com.y271727uy.FRMC.mixin.citadel.tick;

import com.github.alexthe666.citadel.server.world.CitadelServerData;
import com.y271727uy.FRMC.integration.citadel.tick.CitadelDataCache;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 缓存 CitadelServerData.get 的结果，跳过每次的 DimensionDataStorage 查找（保持原版每次 setDirty 的行为）。 */
@Pseudo
@Mixin(value = CitadelServerData.class, remap = false)
public abstract class CitadelServerDataMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void frmc$getCached(MinecraftServer server, CallbackInfoReturnable<CitadelServerData> cir) {
        CitadelServerData cached = CitadelDataCache.lookup(server);
        if (cached != null) {
            cached.setDirty();
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "get", at = @At("RETURN"))
    private static void frmc$storeInCache(MinecraftServer server, CallbackInfoReturnable<CitadelServerData> cir) {
        CitadelServerData data = cir.getReturnValue();
        if (data != null) {
            CitadelDataCache.store(server, data);
        }
    }
}
