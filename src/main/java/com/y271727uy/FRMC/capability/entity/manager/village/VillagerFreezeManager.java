package com.y271727uy.FRMC.capability.entity.manager.village;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.capability.entity.manager.districtpressure.ChunkMobPressureTracker;
import com.y271727uy.FRMC.capability.entity.manager.entityactivity.EntityVisibilityPolicy;
import com.y271727uy.FRMC.capability.entity.manager.entityactivity.MobFreeze;
import com.y271727uy.FRMC.capability.villager.diagnostics.VillagerProfiler;
import com.y271727uy.FRMC.config.EntityActivityConfig;
import com.y271727uy.FRMC.config.VillagerOptimizationConfig;
import com.y271727uy.FRMC.mixin.minecraft.villager.optimization.VillagerMerchantAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Villager-specific replacement for the generic hidden-mob AI sleep. Vanilla villagers are taken out of the
 * generic rules entirely: they keep full AI unless the server is under MSPT pressure, and then only villagers
 * that stand inside a village, are hidden from every player and have nothing urgent going on are frozen.
 *
 * <p>Wake-ups: becoming visible (the visibility packet handler calls EntityActivityManager.wake), being attacked
 * or right-clicked (events below), something urgent at the next evaluation, or pressure being released. After a
 * wake-up a villager stays awake for a minimum time so a villager at the edge of view cannot flap.
 *
 * <p>A villager frozen through work hours cannot reach its workstation to restock, so on wake-up it gets the one
 * restock check vanilla's WorkAtPoi would have made: {@code if (shouldRestock()) restock()}, with all of vanilla's
 * own daily limits and demand updates.
 */
public final class VillagerFreezeManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double HOSTILE_RADIUS_SQR = 16.0D * 16.0D;
    private static final Map<Mob, FrozenAt> FROZEN = new HashMap<>();
    private static final Map<Mob, Long> HIDDEN_SINCE_TICK = new HashMap<>();
    private static final Map<Mob, Long> AWAKE_UNTIL_TICK = new HashMap<>();
    private static VillagePressureGauge gauge;
    private static float lastMspt;

    private VillagerFreezeManager() {
    }

    private record FrozenAt(long gameTime, long dayTime) {
    }

    /** Exactly vanilla villagers; modded subclasses stay under the generic rules. */
    public static boolean manages(Mob mob) {
        return VillagerOptimizationConfig.villageFreeze() && mob.getType() == EntityType.VILLAGER;
    }

    public static boolean isFrozen(Mob mob) {
        return FROZEN.containsKey(mob);
    }

    public static void wake(Mob mob) {
        HIDDEN_SINCE_TICK.remove(mob);
        FrozenAt frozen = FROZEN.remove(mob);
        if (mob.isRemoved()) {
            AWAKE_UNTIL_TICK.remove(mob);
            return;
        }
        if (frozen != null) {
            thaw(mob, frozen);
        }
    }

    /** Pressure released or the feature reset: wake everyone, with the same restock catch-up. */
    public static void clear() {
        HIDDEN_SINCE_TICK.clear();
        if (!FROZEN.isEmpty()) {
            Map<Mob, FrozenAt> frozen = new HashMap<>(FROZEN);
            FROZEN.clear();
            frozen.forEach((mob, at) -> {
                if (!mob.isRemoved()) {
                    thaw(mob, at);
                }
            });
        }
    }

    /** Samples server load once per evaluation pass. */
    public static void updatePressure(MinecraftServer server) {
        if (!VillagerOptimizationConfig.villageFreeze()) {
            return;
        }
        if (gauge == null) {
            gauge = new VillagePressureGauge(VillagerOptimizationConfig.villageFreezeMspt(),
                    VillagerOptimizationConfig.villageFreezeReleaseMspt(),
                    VillagerOptimizationConfig.villageFreezeHoldTicks());
        }
        boolean before = gauge.high();
        lastMspt = server.getAverageTickTime();
        boolean now = gauge.update(lastMspt, server.getTickCount());
        if (before != now) {
            LOGGER.info("Village freeze pressure {} at {} MSPT (frozen villagers: {})",
                    now ? "engaged" : "released", String.format("%.1f", lastMspt), FROZEN.size());
            if (!now) {
                clear();
            }
        }
        if (!AWAKE_UNTIL_TICK.isEmpty()) {
            AWAKE_UNTIL_TICK.entrySet().removeIf(entry -> entry.getKey().isRemoved()
                    || entry.getKey().level().getGameTime() >= entry.getValue());
        }
    }

    public static void evaluate(ServerLevel level, Mob mob, List<ServerPlayer> players) {
        if (gauge == null || !gauge.high() || mob.isNoAi() || players.isEmpty()
                || !EntityVisibilityPolicy.isHiddenFromAll(players, mob.getId())) {
            wake(mob);
            return;
        }
        long now = level.getGameTime();
        long since = HIDDEN_SINCE_TICK.computeIfAbsent(mob, ignored -> now);
        if (FROZEN.containsKey(mob)) {
            if (mustStayAwake(level, mob)) {
                wake(mob);
            }
            return;
        }
        Long awakeUntil = AWAKE_UNTIL_TICK.get(mob);
        if (now - since < EntityActivityConfig.hiddenGraceTicks()
                || (awakeUntil != null && now < awakeUntil)
                || !level.isVillage(mob.blockPosition())
                || mustStayAwake(level, mob)) {
            return;
        }
        // Unlike the generic sleep, keep the navigation path: on wake navigation.tick resumes it in the same
        // tick, instead of MoveToTargetSink seeing a finished path, dropping the walk target and idling.
        FROZEN.put(mob, new FrozenAt(now, level.getDayTime()));
        MobFreeze.haltInputs(mob);
    }

    /** Any attack, including fall, fire and lava damage that has no attacker to remember. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && FROZEN.containsKey(mob)) {
            wake(mob);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Mob mob && !mob.level().isClientSide() && FROZEN.containsKey(mob)) {
            wake(mob);
        }
    }

    public static Status status() {
        VillagePressureGauge current = gauge;
        return new Status(VillagerOptimizationConfig.villageFreeze(), lastMspt,
                current == null ? VillagerOptimizationConfig.villageFreezeMspt() : current.engageMspt(),
                current == null ? VillagerOptimizationConfig.villageFreezeReleaseMspt() : current.releaseMspt(),
                current != null && current.high(), FROZEN.size());
    }

    public record Status(boolean enabled, float mspt, float engageMspt, float releaseMspt, boolean pressure,
                         int frozen) {
    }

    private static void thaw(Mob mob, FrozenAt frozen) {
        long now = mob.level().getGameTime();
        AWAKE_UNTIL_TICK.put(mob, now + VillagerOptimizationConfig.villageFreezeMinAwakeTicks());
        if (VillagerOptimizationConfig.villageFreezeRestockCatchUp() && mob instanceof Villager villager) {
            catchUpRestock(villager, frozen, now);
        }
    }

    /** Exactly the check WorkAtPoi makes at the workstation, applied once if the freeze covered work hours. */
    private static void catchUpRestock(Villager villager, FrozenAt frozen, long now) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        Brain<Villager> brain = villager.getBrain();
        if (villager.isBaby() || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT
                || !brain.hasMemoryValue(MemoryModuleType.JOB_SITE)
                || !FrozenWindow.overlaps(frozen.dayTime(), now - frozen.gameTime(),
                        dayTime -> brain.getSchedule().getActivityAt(dayTime) == Activity.WORK)) {
            return;
        }
        if (villager.shouldRestock()) {
            villager.restock();
            VillagerProfiler.recordOptimization(villager, "village-freeze-restock-catch-up");
        }
    }

    /**
     * Anything a frozen brain would miss. Sensor-fed memories go stale while frozen, so these use entity and
     * world state that keeps updating: the base entity tick still runs for frozen mobs.
     */
    private static boolean mustStayAwake(ServerLevel level, Mob mob) {
        if (mob instanceof AbstractVillager villager && villager.isTrading()) {
            return true;
        }
        // The post-trade level-up countdown only advances while the AI runs.
        if (mob instanceof VillagerMerchantAccessor merchant && merchant.frmc$updateMerchantTimer() > 0) {
            return true;
        }
        if (mob.hurtTime > 0 || mob.getLastHurtByMob() != null || mob.isOnFire()) {
            return true;
        }
        // Being led or carried by a player.
        if (mob.isLeashed() || mob.isPassenger() || mob.isVehicle()) {
            return true;
        }
        Brain<?> brain = mob.getBrain();
        if (brain.isActive(Activity.PANIC) || brain.isActive(Activity.HIDE)
                || brain.isActive(Activity.RAID) || brain.isActive(Activity.PRE_RAID)) {
            return true;
        }
        BlockPos pos = mob.blockPosition();
        return level.isRaided(pos) || hostileNearby(level, mob);
    }

    /** Uses the chunk pressure index refreshed at the start of this evaluation pass; no entity queries. */
    private static boolean hostileNearby(ServerLevel level, Mob mob) {
        ChunkPos center = mob.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkMobPressureTracker.ChunkPressure pressure =
                        ChunkMobPressureTracker.pressure(level, ChunkPos.asLong(center.x + dx, center.z + dz));
                if (pressure == null || pressure.targetModMobs() + pressure.nonTargetHostileMobs() == 0) {
                    continue;
                }
                for (Mob other : pressure.mobs()) {
                    if (other.getType().getCategory() == MobCategory.MONSTER && !other.isRemoved()
                            && other.distanceToSqr(mob) <= HOSTILE_RADIUS_SQR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
