package com.y271727uy.FRMC.integration.citadel.render.ar;

public interface ICitadelGpuBuffers {
    CitadelGpuPoseBatch frmc$citadelPoseBatch();

    int frmc$reserveCitadelPoses(int count);
}
