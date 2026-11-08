package com.y271727uy.FRMC.integration.terrablender;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class AreaCacheDiagnostics {
    private static final ForgeConfigSpec CONFIG;
    private static final ForgeConfigSpec.BooleanValue AREA_CACHE_DIAGNOSTICS;
    private static final ForgeConfigSpec.BooleanValue CACHE_SURFACE_RULE_DISPATCH;
    private static final ForgeConfigSpec.BooleanValue CACHE_SECOND_SURFACE_BIOME;
    private static final ForgeConfigSpec.BooleanValue SKIP_INACTIVE_SURFACE_DISPATCH;
    private static final ForgeConfigSpec.BooleanValue SURFACE_BUILD_DIAGNOSTICS;
    private static final ForgeConfigSpec.BooleanValue SURFACE_DISPATCH_DIAGNOSTICS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        AREA_CACHE_DIAGNOSTICS = builder
            .comment("Log sampled TerraBlender Area cache statistics during world generation. Restart to apply changes.")
            .define("areaCacheDiagnostics", false);
        CACHE_SURFACE_RULE_DISPATCH = builder
            .comment("Reuse the selected TerraBlender surface rule while the biome is unchanged. Restart to apply changes.")
            .define("cacheSurfaceRuleDispatch", false);
        CACHE_SECOND_SURFACE_BIOME = builder
            .comment("Keep a second biome in the surface rule dispatch cache. Requires cacheSurfaceRuleDispatch. Restart to apply changes.")
            .define("cacheSecondSurfaceBiome", false);
        SKIP_INACTIVE_SURFACE_DISPATCH = builder
            .comment("Skip biome dispatch when all namespace rules are gated off by vanilla terrain conditions. Requires cacheSurfaceRuleDispatch. Restart to apply changes.")
            .define("skipInactiveSurfaceDispatch", false);
        SURFACE_BUILD_DIAGNOSTICS = builder
            .comment("Log chunk surface build times for A/B testing. Restart to apply changes.")
            .define("surfaceBuildDiagnostics", false);
        SURFACE_DISPATCH_DIAGNOSTICS = builder
            .comment("Log surface rule dispatch cache hit counts from sampled contexts. Requires cacheSurfaceRuleDispatch. Restart to apply changes.")
            .define("surfaceDispatchDiagnostics", false);
        CONFIG = builder.build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-TerraBlender");
    private static final int SAMPLE_INTERVAL = 64;
    private static final long REPORT_INTERVAL_NS = TimeUnit.SECONDS.toNanos(10);
    private static final LongAdder OPTIMISTIC_HITS = new LongAdder();
    private static final LongAdder READ_LOCKS = new LongAdder();
    private static final LongAdder READ_WAIT_NS = new LongAdder();
    private static final LongAdder WRITE_LOCKS = new LongAdder();
    private static final LongAdder WRITE_WAIT_NS = new LongAdder();
    private static final LongAdder MISSES = new LongAdder();
    private static final LongAdder SURFACE_BUILDS = new LongAdder();
    private static final LongAdder SURFACE_BUILD_NS = new LongAdder();
    private static final LongAdder FIRST_SURFACE_HITS = new LongAdder();
    private static final LongAdder SECOND_SURFACE_HITS = new LongAdder();
    private static final LongAdder SURFACE_NAMESPACE_LOOKUPS = new LongAdder();
    private static final LongAdder UNCACHED_SURFACE_HOLDERS = new LongAdder();
    private static final LongAdder SURFACE_DISPATCH_BYPASSES = new LongAdder();
    private static final LongAdder SURFACE_GUARDED_CONTEXTS = new LongAdder();
    private static final LongAdder SURFACE_UNGUARDED_CONTEXTS = new LongAdder();

    private static volatile boolean enabled;
    private static volatile boolean cacheSurfaceRuleDispatch;
    private static volatile boolean cacheSecondSurfaceBiome;
    private static volatile boolean skipInactiveSurfaceDispatch;
    private static volatile boolean surfaceBuildDiagnostics;
    private static volatile boolean surfaceDispatchDiagnostics;
    private static long lastReport = System.nanoTime();

    private AreaCacheDiagnostics() {
    }

    @SuppressWarnings("removal")
    public static void register(IEventBus modEventBus) {
        if (ModList.get().isLoaded("terrablender")) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG, "frmc/frmc-terrablender.toml");
            modEventBus.addListener(AreaCacheDiagnostics::onConfigLoading);
            MinecraftForge.EVENT_BUS.addListener(AreaCacheDiagnostics::onServerTick);
        }
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != CONFIG) {
            return;
        }
        enabled = AREA_CACHE_DIAGNOSTICS.get();
        cacheSurfaceRuleDispatch = CACHE_SURFACE_RULE_DISPATCH.get();
        cacheSecondSurfaceBiome = cacheSurfaceRuleDispatch && CACHE_SECOND_SURFACE_BIOME.get();
        skipInactiveSurfaceDispatch = cacheSurfaceRuleDispatch && SKIP_INACTIVE_SURFACE_DISPATCH.get();
        surfaceBuildDiagnostics = SURFACE_BUILD_DIAGNOSTICS.get();
        surfaceDispatchDiagnostics = cacheSurfaceRuleDispatch && SURFACE_DISPATCH_DIAGNOSTICS.get();
        if (enabled) {
            LOGGER.info("Area cache diagnostics enabled, sampling 1 in {} lookups on average", SAMPLE_INTERVAL);
        }
        if (cacheSurfaceRuleDispatch) {
            LOGGER.info("Surface rule dispatch cache enabled, entries={}, skipInactiveDispatch={}",
                cacheSecondSurfaceBiome ? 2 : 1, skipInactiveSurfaceDispatch);
        }
        if (surfaceDispatchDiagnostics) {
            LOGGER.info("Surface rule dispatch diagnostics enabled, sampling 1 in {} contexts on average", SAMPLE_INTERVAL);
        }
    }

    public static boolean cacheSurfaceRuleDispatch() {
        return cacheSurfaceRuleDispatch;
    }

    public static boolean cacheSecondSurfaceBiome() {
        return cacheSecondSurfaceBiome;
    }

    public static boolean skipInactiveSurfaceDispatch() {
        return skipInactiveSurfaceDispatch;
    }

    public static boolean surfaceBuildDiagnostics() {
        return surfaceBuildDiagnostics;
    }

    public static boolean shouldSampleSurfaceDispatch() {
        return surfaceDispatchDiagnostics && ThreadLocalRandom.current().nextInt(SAMPLE_INTERVAL) == 0;
    }

    public static void recordFirstSurfaceHit() {
        FIRST_SURFACE_HITS.increment();
    }

    public static void recordSecondSurfaceHit() {
        SECOND_SURFACE_HITS.increment();
    }

    public static void recordSurfaceNamespaceLookup() {
        SURFACE_NAMESPACE_LOOKUPS.increment();
    }

    public static void recordUncachedSurfaceHolder() {
        UNCACHED_SURFACE_HOLDERS.increment();
    }

    public static void recordSurfaceGuardContext(boolean guarded) {
        (guarded ? SURFACE_GUARDED_CONTEXTS : SURFACE_UNGUARDED_CONTEXTS).increment();
    }

    public static void recordSurfaceDispatchBypass() {
        SURFACE_DISPATCH_BYPASSES.increment();
    }

    public static void recordSurfaceBuild(long elapsedNs) {
        SURFACE_BUILD_NS.add(elapsedNs);
        SURFACE_BUILDS.increment();
    }

    public static boolean shouldSample() {
        return enabled && ThreadLocalRandom.current().nextInt(SAMPLE_INTERVAL) == 0;
    }

    public static void recordOptimisticHit() {
        OPTIMISTIC_HITS.increment();
    }

    public static void recordReadLock(long waitNs) {
        READ_LOCKS.increment();
        READ_WAIT_NS.add(waitNs);
    }

    public static void recordWriteLock(long waitNs, boolean miss) {
        WRITE_LOCKS.increment();
        WRITE_WAIT_NS.add(waitNs);
        if (miss) {
            MISSES.increment();
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if ((!enabled && !surfaceBuildDiagnostics && !surfaceDispatchDiagnostics) || event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastReport < REPORT_INTERVAL_NS) {
            return;
        }
        lastReport = now;

        if (enabled) {
            reportAreaCache();
        }
        if (surfaceBuildDiagnostics) {
            long builds = SURFACE_BUILDS.sumThenReset();
            long buildNs = SURFACE_BUILD_NS.sumThenReset();
            if (builds > 0) {
                LOGGER.info("Surface build dispatchCache={} cacheEntries={} chunks={} avgUsPerChunk={} skipInactiveDispatch={}",
                    cacheSurfaceRuleDispatch, cacheSecondSurfaceBiome ? 2 : cacheSurfaceRuleDispatch ? 1 : 0,
                    builds, buildNs / builds / 1_000, skipInactiveSurfaceDispatch);
            }
        }
        if (surfaceDispatchDiagnostics) {
            reportSurfaceDispatch();
        }
    }

    private static void reportSurfaceDispatch() {
        long firstHits = FIRST_SURFACE_HITS.sumThenReset();
        long secondHits = SECOND_SURFACE_HITS.sumThenReset();
        long namespaceLookups = SURFACE_NAMESPACE_LOOKUPS.sumThenReset();
        long uncachedHolders = UNCACHED_SURFACE_HOLDERS.sumThenReset();
        long bypasses = SURFACE_DISPATCH_BYPASSES.sumThenReset();
        long guardedContexts = SURFACE_GUARDED_CONTEXTS.sumThenReset();
        long unguardedContexts = SURFACE_UNGUARDED_CONTEXTS.sumThenReset();
        long calls = firstHits + secondHits + namespaceLookups + uncachedHolders + bypasses;
        if (calls > 0 || guardedContexts > 0 || unguardedContexts > 0) {
            LOGGER.info("Surface dispatch cacheEntries={} calls={} firstHits={} secondHits={} namespaceLookups={} uncachedHolders={} bypassedDispatch={} guardedContexts={} unguardedContexts={}",
                cacheSecondSurfaceBiome ? 2 : 1, calls, firstHits, secondHits, namespaceLookups, uncachedHolders,
                bypasses, guardedContexts, unguardedContexts);
        }
    }

    private static void reportAreaCache() {
        long optimisticHits = OPTIMISTIC_HITS.sumThenReset();
        long readLocks = READ_LOCKS.sumThenReset();
        long readWaitNs = READ_WAIT_NS.sumThenReset();
        long writeLocks = WRITE_LOCKS.sumThenReset();
        long writeWaitNs = WRITE_WAIT_NS.sumThenReset();
        long misses = MISSES.sumThenReset();
        long samples = optimisticHits + readLocks;
        if (samples > 0 || writeLocks > 0) {
            LOGGER.info("Area cache samples={} optimisticHits={} readLocks={} readAcquireAvgNs={} writeLocks={} writeAcquireAvgNs={} misses={}",
                samples, optimisticHits, readLocks, readLocks == 0 ? 0 : readWaitNs / readLocks,
                writeLocks, writeLocks == 0 ? 0 : writeWaitNs / writeLocks, misses);
        }
    }
}
