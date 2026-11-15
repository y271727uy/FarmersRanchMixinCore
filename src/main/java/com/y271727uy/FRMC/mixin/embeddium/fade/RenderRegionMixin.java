package com.y271727uy.FRMC.mixin.embeddium.fade;

import com.y271727uy.FRMC.integration.embeddium.fade.ChunkFadeIn;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeRegion;
import java.util.Arrays;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = RenderRegion.class, remap = false)
public abstract class RenderRegionMixin implements FadeRegion {
    /** 只在 region 内有 section 正在淡入时存在，淡入结束后释放。 */
    @Unique private int[] frmc$fadeInStarts;
    @Unique private int frmc$fadeInEnd;

    @Override
    public void frmc$startFadeIn(int sectionIndex) {
        if (!ChunkFadeIn.active()) {
            return;
        }

        int now = ChunkFadeIn.now();

        if (this.frmc$fadeInStarts == null) {
            this.frmc$fadeInStarts = new int[RenderRegion.REGION_SIZE];
            // 其余 section 视为早已淡入完成。
            Arrays.fill(this.frmc$fadeInStarts, now - ChunkFadeIn.DURATION_MILLIS);
        }

        this.frmc$fadeInStarts[sectionIndex] = now;
        this.frmc$fadeInEnd = now + ChunkFadeIn.DURATION_MILLIS;
    }

    @Override
    public boolean frmc$isFadingIn(int now) {
        if (this.frmc$fadeInStarts == null) {
            return false;
        }

        // 用差值比较，毫秒计数溢出回绕时也成立。
        if (now - this.frmc$fadeInEnd >= 0) {
            this.frmc$fadeInStarts = null;
            return false;
        }

        return true;
    }

    @Override
    public void frmc$writeFadeIn(float[] progress, int now) {
        int[] starts = this.frmc$fadeInStarts;

        for (int i = 0; i < RenderRegion.REGION_SIZE; i++) {
            progress[i] = ChunkFadeIn.progress(starts[i], now);
        }
    }
}
