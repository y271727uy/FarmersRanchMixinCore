package com.y271727uy.FRMC.mixin.untamed_wilds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.client.untamed_wilds.UntamedWildsCreativeTabRefresh;
import java.util.concurrent.CompletableFuture;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "untamedwilds.network.SyncTextureData", remap = false)
public abstract class SyncTextureDataMixin {
    @WrapOperation(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/network/NetworkEvent$Context;enqueueWork(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"
        ),
        require = 1
    )
    private CompletableFuture<?> frmc$refreshCreativeTabsAfterSpeciesSync(NetworkEvent.Context context, Runnable syncTask, Operation<CompletableFuture<?>> original) {
        return original.call(context, (Runnable) () -> {
            syncTask.run();
            UntamedWildsCreativeTabRefresh.markSpeciesSyncReceived();
        });
    }
}
