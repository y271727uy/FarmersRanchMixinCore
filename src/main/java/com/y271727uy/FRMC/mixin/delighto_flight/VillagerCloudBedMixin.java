package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.registry.DFPoi;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerCloudBedMixin {
    // 与提交pr重合
    @Inject(method = "releasePoi", at = @At("TAIL"))
    private void frmc$releaseCloudBed(MemoryModuleType<GlobalPos> memory, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (memory != MemoryModuleType.HOME || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        villager.getBrain().getMemory(MemoryModuleType.HOME).ifPresent(home -> {
            ServerLevel homeLevel = level.getServer().getLevel(home.dimension());
            if (homeLevel != null) {
                PoiManager pois = homeLevel.getPoiManager();
                if (pois.existsAtPosition(DFPoi.CLOUD_BED.getKey(), home.pos()) && pois.release(home.pos())) {
                    DebugPackets.sendPoiTicketCountPacket(homeLevel, home.pos());
                }
            }
        });
    }
}
