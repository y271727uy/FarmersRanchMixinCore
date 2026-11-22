package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.event.ServerEvents;
import com.github.alexthe666.alexsmobs.world.BeachedCachalotWhaleSpawner;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** 读取 Alex's Mobs 搁浅抹香鲸生成器的世界缓存，原版从不清理。 */
@Mixin(value = ServerEvents.class, remap = false)
public interface AMServerEventsAccessor {
    @Accessor("BEACHED_CACHALOT_WHALE_SPAWNER_MAP")
    static Map<ServerLevel, BeachedCachalotWhaleSpawner> frmc$beachedWhaleSpawners() {
        throw new AssertionError();
    }
}
