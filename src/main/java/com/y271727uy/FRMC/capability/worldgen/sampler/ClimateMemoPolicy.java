package com.y271727uy.FRMC.capability.worldgen.sampler;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.DensityFunctionMarkerAccessor;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.DensityFunctionNoiseAccessor;
import com.y271727uy.FRMC.mixin.minecraft.worldgen.DensityFunctionShiftedNoiseAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;

import java.util.Set;

/**
 * 决定 {@code RandomState} 构造气候 sampler 时，哪些被剥掉的缓存标记可以换成 {@link XzMemoDensityFunction}。
 *
 * <p>只考虑原版语义本身就是二维缓存的 {@code flat_cache} 和 {@code cache_2d}，并且要求静态证明子树与 y 无关
 * （绑定时间分析）：子树里每个节点都必须是"自身不读 y"的已知类型。任何不认识的节点（包括其他模组的类型）
 * 都视为依赖 y，这个标记就照原版剥掉，因此结果永远与原版一致。
 *
 * <p>1.20.1 Forge 运行时使用 Mojang 官方类名，包私有的节点类型按类名识别。
 */
public final class ClimateMemoPolicy {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DF = "net.minecraft.world.level.levelgen.DensityFunctions$";

    /** 自身不读 y 的节点：常数、四则/映射/截断/区间选择/样条等组合节点、只读 x/z 的 shift、在这条路径上恒为常数的混合节点。 */
    private static final Set<String> Y_FREE_NODES = Set.of(
        DF + "Constant",
        DF + "Ap2",
        DF + "MulOrAdd",
        DF + "Mapped",
        DF + "Clamp",
        DF + "RangeChoice",
        DF + "Spline",
        DF + "HolderHolder",
        DF + "ShiftA",
        DF + "ShiftB",
        DF + "BlendAlpha",
        DF + "BlendOffset",
        DF + "Marker"
    );
    private static final String MARKER = DF + "Marker";
    private static final String NOISE = DF + "Noise";
    private static final String SHIFTED_NOISE = DF + "ShiftedNoise";
    private static final ResourceLocation FLAT_CACHE = new ResourceLocation("minecraft", "flat_cache");
    private static final ResourceLocation CACHE_2D = new ResourceLocation("minecraft", "cache_2d");

    private ClimateMemoPolicy() {
    }

    public static boolean isMarker(DensityFunction function) {
        return MARKER.equals(function.getClass().getName());
    }

    /** 调用方需先用 {@link #isMarker} 确认。 */
    public static DensityFunction markerWrapped(DensityFunction marker) {
        return ((DensityFunctionMarkerAccessor) (Object) marker).frmc$wrapped();
    }

    /**
     * 是可以记忆的二维缓存标记时，返回包装好的 {@link XzMemoDensityFunction}；否则返回 null，由调用方照原版剥掉。
     * 标记类型按 codec 在 {@code DENSITY_FUNCTION_TYPE} 里的注册名判断，不引用包私有的枚举。
     */
    public static DensityFunction memoize(DensityFunction function) {
        if (!isMarker(function)) {
            return null;
        }
        try {
            ResourceLocation type = BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(function.codec().codec());
            if (!FLAT_CACHE.equals(type) && !CACHE_2D.equals(type)) {
                return null;
            }
            DensityFunction wrapped = markerWrapped(function);
            return isYIndependent(wrapped) ? new XzMemoDensityFunction(wrapped) : null;
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("[FRMC] Climate memo analysis failed, keeping vanilla behaviour for {}", function, e);
            return null;
        }
    }

    /** 整棵子树都只由"自身不读 y"的节点组成时返回 true。 */
    public static boolean isYIndependent(DensityFunction root) {
        boolean[] dependent = {false};
        root.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                if (!dependent[0] && !isYFreeNode(function)) {
                    dependent[0] = true;
                }
                return function;
            }
        });
        return !dependent[0];
    }

    private static boolean isYFreeNode(DensityFunction function) {
        if (function instanceof XzMemoDensityFunction) {
            return true;
        }
        String type = function.getClass().getName();
        if (NOISE.equals(type)) {
            return ((DensityFunctionNoiseAccessor) (Object) function).frmc$yScale() == 0.0;
        }
        if (SHIFTED_NOISE.equals(type)) {
            // shiftX/shiftY/shiftZ 作为子节点会被单独访问。
            return ((DensityFunctionShiftedNoiseAccessor) (Object) function).frmc$yScale() == 0.0;
        }
        // 标记本身不读坐标，被包装的子树会被单独访问。
        return Y_FREE_NODES.contains(function.getClass().getName());
    }
}
