package com.y271727uy.FRMC.integration.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntitySnowLeopard;
import com.github.alexthe666.alexsmobs.entity.EntityTiger;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Alex's Mobs gives every creeper two {@code AvoidEntityGoal}s, one for snow leopards and one for tigers.
 * Each evaluation scans the entity sections around the creeper and may then plan a flee path. With no
 * such animal loaded in the level, both scans are guaranteed to find nothing.
 *
 * <p>The count is only ever allowed to err high (a join cancelled after this listener), which merely
 * keeps the original scan running. Joins cancelled before it are not counted, and it never goes below zero.
 */
public final class CreeperPredatorTracker {
    private static final Map<LevelAccessor, AtomicInteger> LOADED = new ConcurrentHashMap<>();

    private CreeperPredatorTracker() {
    }

    public static void register() {
        if (!ModList.get().isLoaded("alexsmobs")) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CreeperPredatorTracker::onJoin);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CreeperPredatorTracker::onLeave);
        MinecraftForge.EVENT_BUS.addListener(CreeperPredatorTracker::onUnload);
    }

    /** True when the avoid class is one of the tracked predators and none is loaded in this level. */
    public static boolean canSkip(Class<?> avoidClass, Level level) {
        if (avoidClass != EntitySnowLeopard.class && avoidClass != EntityTiger.class) {
            return false;
        }
        AtomicInteger count = LOADED.get(level);
        return count == null || count.get() <= 0;
    }

    private static boolean isTracked(Entity entity) {
        return entity instanceof EntitySnowLeopard || entity instanceof EntityTiger;
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (!level.isClientSide && isTracked(event.getEntity())) {
            LOADED.computeIfAbsent(level, key -> new AtomicInteger()).incrementAndGet();
        }
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        Level level = event.getLevel();
        if (!level.isClientSide && isTracked(event.getEntity())) {
            AtomicInteger count = LOADED.get(level);
            if (count != null) {
                count.updateAndGet(value -> value > 0 ? value - 1 : 0);
            }
        }
    }

    private static void onUnload(LevelEvent.Unload event) {
        LOADED.remove(event.getLevel());
    }
}
