package com.y271727uy.FRMC.mixin.untamed_wilds;

import com.y271727uy.FRMC.client.untamed_wilds.UntamedWildsCreativeTabRefresh;
import java.util.concurrent.CompletableFuture;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "untamedwilds.network.SyncTextureData", remap = false)
public abstract class SyncTextureDataMixin {
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/network/NetworkEvent$Context;enqueueWork(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"
        ),
        require = 1
    )
    private CompletableFuture<?> frmc$refreshCreativeTabsAfterSpeciesSync(NetworkEvent.Context context, Runnable syncTask) {
        return context.enqueueWork(() -> {
            syncTask.run();
            UntamedWildsCreativeTabRefresh.markSpeciesSyncReceived();
        });
    }
}
