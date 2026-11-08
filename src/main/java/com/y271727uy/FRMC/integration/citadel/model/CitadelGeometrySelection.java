package com.y271727uy.FRMC.integration.citadel.model;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;

/** Selects mesh geometry using the original skeleton indices. */
public final class CitadelGeometrySelection {
    private CitadelGeometrySelection() {
    }

    /** Returns the geometry visible through showModel in a validated tree of up to 64 AdvancedModelBoxes. */
    public static long visibleGeometry(CitadelSkeletonLayoutCache.Layout layout) {
        BasicModelPart[] nodes = layout.nodes();
        int[] subtreeEnds = layout.topology().subtreeEnds();
        long geometry = 0;
        for (int i = 0; i < nodes.length; ) {
            AdvancedModelBox node = (AdvancedModelBox) nodes[i];
            if (!node.showModel) {
                i = subtreeEnds[i];
                continue;
            }
            if (!node.cubeList.isEmpty()) {
                geometry |= 1L << i;
            }
            i++;
        }
        return geometry;
    }

    /** Removes only the selected nodes' own geometry, keeping the full mesh if it would become empty. */
    public static long omitOwnGeometry(long visibleGeometry, long omittedParts) {
        long reducedGeometry = visibleGeometry & ~omittedParts;
        return reducedGeometry != 0 ? reducedGeometry : visibleGeometry;
    }
}
