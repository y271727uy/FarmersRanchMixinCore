package com.y271727uy.FRMC.mixin.minecraft.server;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 供 {@code GhostPlayerSweeper} 找出并移除残留在区块加载表里的已移除玩家。 */
@Mixin(ChunkMap.class)
public interface ChunkMapGhostPlayerAccessor {
    @Accessor("playerMap")
    PlayerMap frmc$playerMap();

    @Invoker("removeEntity")
    void frmc$removeEntity(Entity entity);
}
