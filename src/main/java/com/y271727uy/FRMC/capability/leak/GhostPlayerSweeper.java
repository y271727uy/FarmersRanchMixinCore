package com.y271727uy.FRMC.capability.leak;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.mixin.minecraft.server.ChunkMapGhostPlayerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理"幽灵玩家"：已经被移除、也不在玩家列表里，却仍登记在 {@code ChunkMap.playerMap} 里的 {@link ServerPlayer}。
 * 这种对象通常是某个模组移除玩家时绕过了正常流程，它会一直持有区块票据，让死亡/离开位置周围
 * 几百个区块持续加载、持续刷怪，直到重启。
 *
 * <p>比 CoO 更保守：必须同时满足"实体已被移除"和"玩家列表里不是它"才清理，
 * 所以正常玩家、重生交接中的玩家、以及挂在玩家列表里的假玩家都不会被碰到。
 * 每 5 秒在服务器 tick 末尾扫描一次，此时重生、换维度都已经完成。
 */
public final class GhostPlayerSweeper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SWEEP_INTERVAL_TICKS = 100;

    private static int ticks;

    private GhostPlayerSweeper() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(GhostPlayerSweeper::onServerTick);
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++ticks < SWEEP_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        PlayerList players = server.getPlayerList();
        for (ServerLevel level : server.getAllLevels()) {
            sweep(level, players);
        }
    }

    private static void sweep(ServerLevel level, PlayerList players) {
        ChunkMapGhostPlayerAccessor chunkMap = (ChunkMapGhostPlayerAccessor) level.getChunkSource().chunkMap;
        List<ServerPlayer> ghosts = null;
        // PlayerMap.getPlayers 忽略参数，返回该维度登记的全部玩家。
        for (ServerPlayer player : chunkMap.frmc$playerMap().getPlayers(0L)) {
            if (player.isRemoved() && players.getPlayer(player.getUUID()) != player) {
                if (ghosts == null) {
                    ghosts = new ArrayList<>();
                }
                ghosts.add(player);
            }
        }
        if (ghosts == null) {
            return;
        }
        for (ServerPlayer ghost : ghosts) {
            LOGGER.warn("[FRMC] Releasing ghost player {} in {} at {} {} {}: it was removed without releasing its chunk tickets",
                ghost.getGameProfile().getName(), level.dimension().location(),
                ghost.getBlockX(), ghost.getBlockY(), ghost.getBlockZ());
            chunkMap.frmc$removeEntity(ghost);
        }
    }
}
