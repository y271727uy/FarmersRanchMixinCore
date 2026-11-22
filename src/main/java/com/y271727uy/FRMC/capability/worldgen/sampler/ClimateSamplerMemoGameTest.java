package com.y271727uy.FRMC.capability.worldgen.sampler;

import com.y271727uy.FRMC.FRMCMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 验证 {@link XzMemoDensityFunction} 接入后，主世界气候 sampler 的结果与原版逐位一致，并测量加速比。
 *
 * <p>参照组按原版 {@code RandomState$1} 的做法从同一个 router 剥掉全部缓存标记，不经过 FRMC 的 mixin。
 * 只在 gametest 环境下注册（{@code forge.enabledGameTestNamespaces} 包含 frmc）。
 */
@GameTestHolder(FRMCMod.MODID)
@PrefixGameTestTemplate(false)
public final class ClimateSamplerMemoGameTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-ClimateMemo");
    private static final long[] SEEDS = {20261117L, -4172144997902289642L, 1L};
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    private ClimateSamplerMemoGameTest() {
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void memoizedSamplerMatchesVanilla(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        NoiseGeneratorSettings settings = registries.registryOrThrow(Registries.NOISE_SETTINGS)
            .getOrThrow(NoiseGeneratorSettings.OVERWORLD);
        HolderGetter<NormalNoise.NoiseParameters> noises = registries.lookupOrThrow(Registries.NOISE);

        long compared = 0;
        for (long seed : SEEDS) {
            RandomState state = RandomState.create(settings, noises, seed);
            Sampler memo = Sampler.of(state.sampler());
            Sampler vanilla = Sampler.of(referenceSampler(state.router()));

            int memoNodes = countMemoNodes(memo);
            if (memoNodes == 0) {
                helper.fail("No XzMemoDensityFunction installed in the overworld climate sampler (seed " + seed + ")");
                return;
            }
            LOGGER.info("seed {}: {} distinct memo nodes shared across the 6 climate functions", seed, memoNodes);

            SplittableRandom random = new SplittableRandom(seed);
            List<int[]> points = new ArrayList<>();
            addScattered(points, random, 20_000);
            addColumns(points, random, 600, 24);
            addCaveTriples(points, random, 4_000);

            String error = compareAll(memo, vanilla, points);
            if (error == null) {
                error = compareConcurrently(memo, vanilla, scattered(new SplittableRandom(seed ^ 0x5DEECE66DL), 20_000));
            }
            if (error != null) {
                helper.fail("seed " + seed + ": " + error);
                return;
            }
            compared += points.size() + 20_000L;
        }
        LOGGER.info("Climate memo matched vanilla bit-for-bit on {} sample points (x6 functions)", compared);

        benchmark(settings, noises);
        helper.succeed();
    }

    // ---- 参照组：与原版 RandomState$1 相同的剥标记逻辑 ----

    private static Climate.Sampler referenceSampler(NoiseRouter router) {
        DensityFunction.Visitor strip = new DensityFunction.Visitor() {
            private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

            @Override
            public DensityFunction apply(DensityFunction function) {
                return wrapped.computeIfAbsent(function, this::unwrap);
            }

            private DensityFunction unwrap(DensityFunction function) {
                if (function instanceof DensityFunctions.HolderHolder holder) {
                    return holder.function().value();
                }
                if (ClimateMemoPolicy.isMarker(function)) {
                    return ClimateMemoPolicy.markerWrapped(function);
                }
                return function;
            }
        };
        return new Climate.Sampler(
            router.temperature().mapAll(strip),
            router.vegetation().mapAll(strip),
            router.continents().mapAll(strip),
            router.erosion().mapAll(strip),
            router.depth().mapAll(strip),
            router.ridges().mapAll(strip),
            List.of());
    }

    private static int countMemoNodes(Sampler sampler) {
        Set<DensityFunction> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (DensityFunction function : sampler.functions) {
            collectMemo(function, seen);
        }
        return seen.size();
    }

    /** 按对象身份收集记忆节点；不用 mapAll，以免它为记忆节点生成新副本。 */
    private static void collectMemo(DensityFunction function, Set<DensityFunction> seen) {
        Map<DensityFunction, Boolean> visited = new IdentityHashMap<>();
        List<DensityFunction> stack = new ArrayList<>();
        stack.add(function);
        while (!stack.isEmpty()) {
            DensityFunction current = stack.remove(stack.size() - 1);
            if (visited.put(current, Boolean.TRUE) != null) {
                continue;
            }
            if (current instanceof XzMemoDensityFunction memo) {
                seen.add(memo);
                stack.add(memo.wrapped());
            }
            for (DensityFunction child : directChildren(current)) {
                stack.add(child);
            }
        }
    }

    /** 通过 record 组件取直接子节点（样条坐标里的节点取不到，只影响统计数量，不影响正确性检查）。 */
    private static List<DensityFunction> directChildren(DensityFunction current) {
        if (current instanceof XzMemoDensityFunction) {
            return List.of();
        }
        List<DensityFunction> children = new ArrayList<>();
        try {
            for (java.lang.reflect.RecordComponent component : recordComponents(current)) {
                Object value = component.getAccessor().invoke(current);
                if (value instanceof DensityFunction child) {
                    children.add(child);
                } else if (value instanceof net.minecraft.core.Holder<?> holder && holder.value() instanceof DensityFunction child) {
                    children.add(child);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 只用于统计记忆节点数量，取不到子节点不影响正确性检查。
        }
        return children;
    }

    private static java.lang.reflect.RecordComponent[] recordComponents(DensityFunction function) {
        Class<?> type = function.getClass();
        if (!type.isRecord()) {
            return new java.lang.reflect.RecordComponent[0];
        }
        java.lang.reflect.RecordComponent[] components = type.getRecordComponents();
        for (java.lang.reflect.RecordComponent component : components) {
            component.getAccessor().setAccessible(true);
        }
        return components;
    }

    // ---- 采样点 ----

    private static int quartBlock(SplittableRandom random, int range) {
        return (random.nextInt(-range, range) >> 2) << 2;
    }

    private static int randomY(SplittableRandom random) {
        return (random.nextInt(MIN_Y, MAX_Y) >> 2) << 2;
    }

    private static void addScattered(List<int[]> points, SplittableRandom random, int count) {
        points.addAll(scattered(random, count));
    }

    private static List<int[]> scattered(SplittableRandom random, int count) {
        List<int[]> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new int[]{quartBlock(random, 30_000), randomY(random), quartBlock(random, 30_000)});
        }
        return points;
    }

    /** 模拟 /locate biome 的三维搜索：同一列多个 y。 */
    private static void addColumns(List<int[]> points, SplittableRandom random, int columns, int levels) {
        for (int c = 0; c < columns; c++) {
            int x = quartBlock(random, 30_000);
            int z = quartBlock(random, 30_000);
            for (int i = 0; i < levels; i++) {
                points.add(new int[]{x, MIN_Y + i * 16, z});
            }
        }
    }

    /** 模拟 Alex's Caves：(x,y,z) → 格中心 (cx,y,cz) → 原版再采一次 (x,y,z)。 */
    private static void addCaveTriples(List<int[]> points, SplittableRandom random, int count) {
        for (int i = 0; i < count; i++) {
            int x = quartBlock(random, 30_000);
            int z = quartBlock(random, 30_000);
            int y = randomY(random);
            int cx = x + quartBlock(random, 256);
            int cz = z + quartBlock(random, 256);
            points.add(new int[]{x, y, z});
            points.add(new int[]{cx, y, cz});
            points.add(new int[]{x, y, z});
        }
    }

    // ---- 比较 ----

    private static String compareAll(Sampler memo, Sampler vanilla, List<int[]> points) {
        for (int[] p : points) {
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            for (int f = 0; f < 6; f++) {
                double a = memo.functions[f].compute(context);
                double b = vanilla.functions[f].compute(context);
                if (Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(b)) {
                    return "function " + Sampler.NAMES[f] + " differs at (" + p[0] + ", " + p[1] + ", " + p[2]
                        + "): memo=" + a + " vanilla=" + b;
                }
            }
        }
        return null;
    }

    private static String compareConcurrently(Sampler memo, Sampler vanilla, List<int[]> points) {
        double[][] expected = new double[points.size()][6];
        for (int i = 0; i < points.size(); i++) {
            int[] p = points.get(i);
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            for (int f = 0; f < 6; f++) {
                expected[i][f] = vanilla.functions[f].compute(context);
            }
        }
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                int offset = t * 977;
                results.add(pool.submit(() -> {
                    for (int n = 0; n < points.size(); n++) {
                        int i = (n + offset) % points.size();
                        int[] p = points.get(i);
                        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
                        for (int f = 0; f < 6; f++) {
                            double a = memo.functions[f].compute(context);
                            if (Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(expected[i][f])) {
                                return "concurrent: function " + Sampler.NAMES[f] + " differs at (" + p[0] + ", "
                                    + p[1] + ", " + p[2] + ")";
                            }
                        }
                    }
                    return null;
                }));
            }
            for (Future<String> result : results) {
                String error = result.get();
                if (error != null) {
                    return error;
                }
            }
            return null;
        } catch (Exception e) {
            return "concurrent comparison failed: " + e;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- 基准 ----

    private static void benchmark(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises) {
        long seed = 987654321L;
        String[] patterns = {"scattered", "column x24", "cave triple"};
        for (int round = 0; round < 3; round++) {
            boolean report = round == 2;
            for (int pattern = 0; pattern < patterns.length; pattern++) {
                RandomState state = RandomState.create(settings, noises, seed + round * 31L + pattern);
                Sampler memo = Sampler.of(state.sampler());
                Sampler vanilla = Sampler.of(referenceSampler(state.router()));
                SplittableRandom random = new SplittableRandom(seed * 7 + round * 131 + pattern);
                List<int[]> points = new ArrayList<>();
                switch (pattern) {
                    case 0 -> addScattered(points, random, 30_000);
                    case 1 -> addColumns(points, random, 1_250, 24);
                    default -> addCaveTriples(points, random, 10_000);
                }
                long vanillaNs = time(vanilla, points);
                long memoNs = time(memo, points);
                if (report) {
                    LOGGER.info("benchmark {}: {} samples, vanilla {} ns/sample, memo {} ns/sample, speedup x{}",
                        patterns[pattern], points.size(), vanillaNs / points.size(), memoNs / points.size(),
                        String.format("%.2f", vanillaNs / (double) memoNs));
                }
            }
        }
    }

    private static long time(Sampler sampler, List<int[]> points) {
        double sink = 0;
        long start = System.nanoTime();
        for (int[] p : points) {
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(p[0], p[1], p[2]);
            for (DensityFunction function : sampler.functions) {
                sink += function.compute(context);
            }
        }
        long elapsed = System.nanoTime() - start;
        if (sink == 42.4242) {
            LOGGER.trace("sink {}", sink);
        }
        return elapsed;
    }

    private record Sampler(DensityFunction[] functions) {
        private static final String[] NAMES = {"temperature", "humidity", "continentalness", "erosion", "depth", "weirdness"};

        static Sampler of(Climate.Sampler sampler) {
            return new Sampler(new DensityFunction[]{
                sampler.temperature(), sampler.humidity(), sampler.continentalness(),
                sampler.erosion(), sampler.depth(), sampler.weirdness()});
        }
    }
}
