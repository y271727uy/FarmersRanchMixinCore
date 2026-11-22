package com.y271727uy.FRMC.capability.leak;

import java.lang.ref.WeakReference;

/**
 * 替换 {@code Climate.RTree.lastResult} 的线程本地缓存，只弱引用上一次命中的叶子。
 *
 * <p>原版的叶子经由 {@code Holder<Biome>} 能一路引用到该世界的注册表视图，而注册表里的
 * {@code LevelStem → ChunkGenerator → BiomeSource} 又引用回这棵 RTree 和这个 ThreadLocal 自身。
 * ThreadLocalMap 的 key 虽然是弱引用，但 value 反向强引用了 key，于是工作线程（比世界活得久）
 * 会把关闭的世界的整个世界生成注册表一直留着。
 *
 * <p>树本身强引用它的所有叶子，所以只要树还能被搜索，这里的弱引用就永远不会被清空，
 * 每次搜索的起点与原版完全相同（包括距离相等时的取舍）；树被回收后叶子才会跟着释放。
 * 叶子没变时不重新分配 WeakReference，热路径上只多一次解引用。
 */
public final class WeakLastResult extends ThreadLocal<Object> {
    @Override
    public Object get() {
        Object ref = super.get();
        return ref == null ? null : ((WeakReference<?>) ref).get();
    }

    @Override
    public void set(Object leaf) {
        Object ref = super.get();
        Object current = ref == null ? null : ((WeakReference<?>) ref).get();
        if (current == leaf && (leaf != null || ref == null)) {
            return;
        }
        super.set(leaf == null ? null : new WeakReference<>(leaf));
    }
}
