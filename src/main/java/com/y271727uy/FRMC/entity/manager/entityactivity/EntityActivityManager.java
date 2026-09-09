package com.y271727uy.FRMC.entity.manager.entityactivity;

import com.y271727uy.FRMC.entity.manager.districtpressure.ChunkMobPressureTracker;
import com.y271727uy.FRMC.entity.manager.mobspawnclean.EntityOverloadCleanupManager;
import com.y271727uy.FRMC.config.EntityActivityConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks server Mobs and decides whether their AI can sleep. */
public final class EntityActivityManager {
    private static final Map<ServerLevel, Set<Mob>> TRACKED = new HashMap<>();
    private static final Set<Mob> SLEEPING = Collections.newSetFromMap(new HashMap<>());
    private static final Set<Mob> THROTTLED = Collections.newSetFromMap(new HashMap<>());
    private static final Map<Mob, Long> HIDDEN_SINCE_TICK = new HashMap<>();
    private static int evaluationCountdown;

    private EntityActivityManager() {
    }

    public static boolean isSleeping(Mob mob) {
        return SLEEPING.contains(mob);
    }

    /**
     * Fully sleeping Mobs skip every AI step. Protected, hidden Mobs in a pressured chunk only
     * skip one distributed step out of five, preserving an average 80% AI frequency.
     */
    public static boolean shouldSkipAiStep(Mob mob) {
        if (SLEEPING.contains(mob)) {
            return true;
        }
        return THROTTLED.contains(mob) && Math.floorMod(mob.tickCount + mob.getId(), 5) == 0;
    }

    public static void wake(Mob mob) {
        restoreFullAi(mob);
        HIDDEN_SINCE_TICK.remove(mob);
    }

    private static void restoreFullAi(Mob mob) {
        SLEEPING.remove(mob);
        THROTTLED.remove(mob);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        TRACKED.computeIfAbsent(level, ignored -> new HashSet<>()).add(mob);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Set<Mob> tracked = TRACKED.get(level);
        if (tracked != null) {
            tracked.remove(mob);
        }
        wake(mob);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!EntityActivityConfig.enabled()) {
            SLEEPING.clear();
            THROTTLED.clear();
            HIDDEN_SINCE_TICK.clear();
            return;
        }
        if (evaluationCountdown > 0) {
            evaluationCountdown--;
            return;
        }
        evaluationCountdown = EntityActivityConfig.evaluationIntervalTicks() - 1;
        evaluateAll();
    }

    private static void evaluateAll() {
        for (var levelEntry : TRACKED.entrySet()) {
            ServerLevel level = levelEntry.getKey();
            List<ServerPlayer> players = activePlayers(level);
            ChunkMobPressureTracker.refresh(level, levelEntry.getValue());
            var iterator = levelEntry.getValue().iterator();
            while (iterator.hasNext()) {
                Mob mob = iterator.next();
                if (mob.isRemoved() || mob.level() != level) {
                    iterator.remove();
                    wake(mob);
                    continue;
                }
                if (!isHiddenFromAll(mob, players)) {
                    wake(mob);
                    continue;
                }

                long hiddenSince = HIDDEN_SINCE_TICK.computeIfAbsent(mob, ignored -> level.getGameTime());
                if (level.getGameTime() - hiddenSince < EntityActivityConfig.hiddenGraceTicks()) {
                    restoreFullAi(mob);
                    continue;
                }

                if (isInProtectionArea(mob, players)) {
                    restoreFullAi(mob);
                    if (ChunkMobPressureTracker.totalMobs(level, mob.chunkPosition().toLong())
                            >= EntityActivityConfig.protectedChunkPressureThreshold()) {
                        THROTTLED.add(mob);
                    }
                } else {
                    mob.getNavigation().stop();
                    THROTTLED.remove(mob);
                    SLEEPING.add(mob);
                }
            }
            EntityOverloadCleanupManager.evaluate(level, players);
        }
    }

    /** Used by the overload cleaner after this evaluation establishes the current hidden duration. */
    public static boolean isCleanupEligible(Mob mob, List<ServerPlayer> players) {
        Long hiddenSince = HIDDEN_SINCE_TICK.get(mob);
        return hiddenSince != null
                && mob.level() instanceof ServerLevel level
                && level.getGameTime() - hiddenSince >= EntityActivityConfig.hiddenGraceTicks()
                && isHiddenFromAll(mob, players)
                && !isInProtectionArea(mob, players);
    }

    private static boolean isHiddenFromAll(Mob mob, List<ServerPlayer> players) {
        return !mob.isNoAi() && !players.isEmpty()
                && EntityVisibilityPolicy.isHiddenFromAll(players, mob.getId());
    }

    private static List<ServerPlayer> activePlayers(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                players.add(player);
            }
        }
        return players;
    }

    private static boolean isInProtectionArea(Mob mob, List<ServerPlayer> players) {
        int protectionChunks = EntityActivityConfig.protectionChunks();
        int mobChunkX = mob.chunkPosition().x;
        int mobChunkZ = mob.chunkPosition().z;
        for (ServerPlayer player : players) {
            if (Math.abs(mobChunkX - player.chunkPosition().x) <= protectionChunks
                    && Math.abs(mobChunkZ - player.chunkPosition().z) <= protectionChunks) {
                return true;
            }
        }
        return false;
    }
}
