package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.network.DFNetworking;
import com.y271727uy.FRMC.integration.delighto_flight.HeartAnimationPayload;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = DFNetworking.class, remap = false)
public abstract class DFNetworkingMixin {
    @Inject(method = "init", at = @At(value = "FIELD",
            target = "Lcom/cloudmeow/delightoflight/network/DFNetworking;INSTANCE:Lnet/minecraftforge/network/simple/SimpleChannel;",
            ordinal = 4), cancellable = true)
    private static void frmc$registerHeartPacketOnDedicatedServer(CallbackInfo ci) {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            // Linking DF's message class itself loads client code. Keep its packet ID and wire format.
            DFNetworking.INSTANCE.registerMessage(4, HeartAnimationPayload.class,
                    (message, buffer) -> buffer.writeItem(message.stack()),
                    buffer -> new HeartAnimationPayload(buffer.readItem()),
                    (message, context) -> context.get().setPacketHandled(true),
                    Optional.of(NetworkDirection.PLAY_TO_CLIENT));
            ci.cancel();
        }
    }

    @Inject(method = "sendHeartAnimation", at = @At("HEAD"), cancellable = true)
    private static void frmc$sendServerHeartPacket(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            DFNetworking.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new HeartAnimationPayload(stack));
            ci.cancel();
        }
    }
}
