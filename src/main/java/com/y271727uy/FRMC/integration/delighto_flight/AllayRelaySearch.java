package com.y271727uy.FRMC.integration.delighto_flight;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;

/** Shared sampling budget for a chef's basket and pot behaviors. */
public final class AllayRelaySearch {
    private static final int SAMPLES_PER_TICK = 16;
    private static final int MAX_SAMPLES = 1000;
    private static final Map<Allay, AllayRelaySearch> SEARCHES = Collections.synchronizedMap(new WeakHashMap<>());

    private long tick = Long.MIN_VALUE;
    private int usedThisTick;
    private MemoryModuleType<GlobalPos> memory;
    private GlobalPos target;
    private int attempts;

    private AllayRelaySearch() {
    }

    public static AllayRelaySearch forAllay(Allay allay) {
        return SEARCHES.computeIfAbsent(allay, ignored -> new AllayRelaySearch());
    }

    public boolean trySample(long gameTime, MemoryModuleType<GlobalPos> workMemory, GlobalPos workTarget) {
        if (tick != gameTime) {
            tick = gameTime;
            usedThisTick = 0;
        }
        if (usedThisTick >= SAMPLES_PER_TICK) {
            return false;
        }
        // Reacquiring a POI creates a new GlobalPos, even when its coordinates match.
        if (memory != workMemory || target != workTarget) {
            memory = workMemory;
            target = workTarget;
            attempts = 0;
        }
        usedThisTick++;
        attempts++;
        return true;
    }

    public boolean isExhausted() {
        return attempts >= MAX_SAMPLES;
    }

    public void finish() {
        memory = null;
        target = null;
        attempts = 0;
    }
}
