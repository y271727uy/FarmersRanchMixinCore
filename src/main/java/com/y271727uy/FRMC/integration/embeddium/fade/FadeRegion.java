package com.y271727uy.FRMC.integration.embeddium.fade;

/** 由 RenderRegionMixin 实现：每个 region 记录其 section 的淡入起始时间。 */
public interface FadeRegion {
    void frmc$startFadeIn(int sectionIndex);

    boolean frmc$isFadingIn(int now);

    void frmc$writeFadeIn(float[] progress, int now);
}
