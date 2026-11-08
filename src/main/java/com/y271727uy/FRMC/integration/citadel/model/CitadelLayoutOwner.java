package com.y271727uy.FRMC.integration.citadel.model;

import com.y271727uy.FRMC.integration.citadel.model.CitadelSkeletonLayoutCache.Layout;

/** Stores a cached skeleton layout on the model root. */
public interface CitadelLayoutOwner {
    Layout frmc$citadelLayout(long revision);

    void frmc$setCitadelLayout(long revision, Layout layout);
}
