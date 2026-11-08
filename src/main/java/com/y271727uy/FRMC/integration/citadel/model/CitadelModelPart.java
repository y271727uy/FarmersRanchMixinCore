package com.y271727uy.FRMC.integration.citadel.model;

import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;

/** Exposes hierarchy data without depending on a render backend. */
public interface CitadelModelPart {
    int FRMC_HAS_GEOMETRY = 1;
    int FRMC_HAS_CHILDREN = 1 << 1;
    int FRMC_ADVANCED = 1 << 2;
    int FRMC_SCALE_CHILDREN = 1 << 3;

    BasicModelPart[] frmc$childrenFast();

    int frmc$layoutFlags();
}
