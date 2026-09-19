package com.y271727uy.FRMC.capability.downland.fetch;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Fetches one URL into a caller-owned file (the {@code .part}).
 * Tests inject a fake; production uses {@link DlcUrlFetcher}.
 */
@FunctionalInterface
public interface DlcHttpClient {
    void fetch(String url, Path dest) throws IOException;
}
