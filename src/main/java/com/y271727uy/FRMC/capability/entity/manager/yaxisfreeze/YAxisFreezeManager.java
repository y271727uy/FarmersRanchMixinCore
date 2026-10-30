package com.y271727uy.FRMC.capability.entity.manager.yaxisfreeze;

import com.y271727uy.FRMC.config.EntityActivityConfig;
import com.y271727uy.FRMC.capability.entity.manager.entityactivity.EntityVisibilityPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns hard AI freezes sourced exclusively from Chloride's distance-culling decision. */
public final class YAxisFreezeManager {
    private static final Set<Mob> FROZEN = Collections.newSetFromMap(new HashMap<>());
    private static final Map<Mob, Long> CULLED_SINCE_TICK = new HashMap<>();

    private YAxisFreezeManager() {
    }

    public static boolean update(Mob mob, List<ServerPlayer> players) {
        if (!isCulledForAll(mob, players)) {
            wake(mob);
            return false;
        }
        ServerLevel level = (ServerLevel) mob.level();
        long since = CULLED_SINCE_TICK.computeIfAbsent(mob, ignored -> level.getGameTime());
        if (level.getGameTime() - since < EntityActivityConfig.hiddenGraceTicks()) {
            FROZEN.remove(mob);
            return false;
        }
        mob.getNavigation().stop();
        FROZEN.add(mob);
        return true;
    }

    public static boolean isFrozen(Mob mob) {
        return FROZEN.contains(mob);
    }

    public static boolean isPending(Mob mob, List<ServerPlayer> players) {
        if (mob == null || FROZEN.contains(mob) || !(mob.level() instanceof ServerLevel level)
                || !isCulledForAll(mob, players)) {
            return false;
        }
        Long since = CULLED_SINCE_TICK.get(mob);
        return since != null && level.getGameTime() - since >= EntityActivityConfig.hiddenGraceTicks();
    }

    public static void wake(Mob mob) {
        FROZEN.remove(mob);
        CULLED_SINCE_TICK.remove(mob);
    }

    public static void clear() {
        FROZEN.clear();
        CULLED_SINCE_TICK.clear();
    }

    private static boolean isCulledForAll(Mob mob, List<ServerPlayer> players) {
        if (mob == null || mob.isNoAi() || players == null || players.isEmpty()
                || !(mob.level() instanceof ServerLevel)) {
            return false;
        }
        return EntityVisibilityPolicy.isDistanceCulledFromAll(players, mob.getId());
    }
}
