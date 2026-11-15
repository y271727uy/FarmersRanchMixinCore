package com.y271727uy.FRMC.integration.embeddium.fade;

import it.unimi.dsi.fastutil.longs.LongSet;

/** 由 RenderSectionManagerMixin 实现：导出已经决定过淡入的 section 坐标。 */
public interface FadeSectionManager {
    LongSet frmc$collectSettledSections();
}
