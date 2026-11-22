package com.y271727uy.FRMC.mixin.minecraft.worldgen;

import com.y271727uy.FRMC.capability.leak.WeakLastResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 见 {@link WeakLastResult}：让关闭世界的生物群系查找树不再被工作线程的 ThreadLocal 留住。 */
@Mixin(targets = "net.minecraft.world.level.biome.Climate$RTree")
public abstract class ClimateRTreeWeakLastResultMixin {
    @Shadow
    @Final
    @Mutable
    private ThreadLocal<?> lastResult;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$useWeakLastResult(CallbackInfo ci) {
        // 只替换原版的普通 ThreadLocal；别的模组已经换过就不动。
        if (this.lastResult != null && this.lastResult.getClass() == ThreadLocal.class) {
            this.lastResult = new WeakLastResult();
        }
    }
}
