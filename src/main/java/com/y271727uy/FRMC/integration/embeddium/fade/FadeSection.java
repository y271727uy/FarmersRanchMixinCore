package com.y271727uy.FRMC.integration.embeddium.fade;

/** 由 RenderSectionMixin 实现：每个 section 只在第一次拿到网格时决定一次是否淡入。 */
public interface FadeSection {
    void frmc$skipFadeIn();

    boolean frmc$isFadeInDecided();
}
