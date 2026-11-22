package com.y271727uy.FRMC.capability.worldgen.sampler;

import com.mojang.logging.LogUtils;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;

/**
 * 长期使用的气候 sampler（{@code RandomState.sampler()}）里，代替被原版剥掉的 {@code flat_cache} / {@code cache_2d} 标记。
 *
 * <p>只包装经 {@link ClimateMemoPolicy} 证明与 y 无关的子树，因此同一 (x, z) 在任何 y 上的结果都相同。
 * 以精确的方块坐标为键，在第一次查询的真实坐标处求值，后续命中直接返回，结果与不加记忆时逐位一致。
 *
 * <p>8 个直接映射槽位，槽里是不可变条目。多线程并发读写最多造成一次未命中：条目的字段都是 final，
 * 读到的条目要么完整、要么是旧的，不会读到半写的值。只存坐标和 double，不引用任何世界对象。
 *
 * <p>JVM 参数 {@code -Dfrmc.climateMemo.verify=true} 时，每次命中都重新计算并逐位比较，不一致就记一次日志。
 */
public final class XzMemoDensityFunction implements DensityFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SLOT_MASK = 7;
    private static final boolean VERIFY = Boolean.getBoolean("frmc.climateMemo.verify");
    private static volatile boolean mismatchLogged;

    public static final KeyDispatchDataCodec<XzMemoDensityFunction> CODEC = KeyDispatchDataCodec.of(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument")
            .xmap(XzMemoDensityFunction::new, XzMemoDensityFunction::wrapped));

    private final DensityFunction wrapped;
    private final Entry[] slots = new Entry[SLOT_MASK + 1];

    public XzMemoDensityFunction(DensityFunction wrapped) {
        this.wrapped = wrapped;
    }

    public DensityFunction wrapped() {
        return wrapped;
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        int slot = slot(x, z);
        Entry entry = slots[slot];
        if (entry != null && entry.x == x && entry.z == z) {
            if (VERIFY) {
                verify(context, entry.value);
            }
            return entry.value;
        }
        double value = wrapped.compute(context);
        slots[slot] = new Entry(x, z, value);
        return value;
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) {
        provider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new XzMemoDensityFunction(wrapped.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return wrapped.minValue();
    }

    @Override
    public double maxValue() {
        return wrapped.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        // 未注册到 DENSITY_FUNCTION_TYPE：sampler 的函数不会被序列化；真被编码时只会得到错误结果而不是崩溃。
        return CODEC;
    }

    private static int slot(int x, int z) {
        int h = x * 0x9E3779B1 ^ z * 0x85EBCA77;
        h ^= h >>> 15;
        return h & SLOT_MASK;
    }

    private void verify(FunctionContext context, double cached) {
        double fresh = wrapped.compute(context);
        if (Double.doubleToRawLongBits(fresh) != Double.doubleToRawLongBits(cached) && !mismatchLogged) {
            mismatchLogged = true;
            LOGGER.error("[FRMC] Climate memo mismatch at ({}, {}, {}): cached={} fresh={} in {}. "
                    + "Disable mixin minecraft.worldgen.RandomStateClimateSamplerMixin and report this.",
                context.blockX(), context.blockY(), context.blockZ(), cached, fresh, wrapped);
        }
    }

    private record Entry(int x, int z, double value) {
    }
}
