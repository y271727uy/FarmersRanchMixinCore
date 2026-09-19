package com.y271727uy.FRMC.capability.downland.fetch;

import com.y271727uy.FRMC.capability.downland.index.DlcDownloadRules;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * HTTPS GET for whitelist CDN URLs. Redirects are followed only when the next
 * URL still passes {@link DlcDownloadRules#isAllowedDownloadUrl(String)}.
 */
public final class DlcUrlFetcher implements DlcHttpClient {
    public static final DlcUrlFetcher INSTANCE = new DlcUrlFetcher();

    private static final int MAX_REDIRECTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String USER_AGENT = "FRMC-DLC";

    private DlcUrlFetcher() {}

    @Override
    public void fetch(String url, Path dest) throws IOException {
        String current = url;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!DlcDownloadRules.isAllowedDownloadUrl(current)) {
                throw new IOException("blocked url");
            }
            HttpURLConnection connection = open(current);
            try {
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        throw new IOException("redirect without location");
                    }
                    current = URI.create(current).resolve(location).toString();
                    continue;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new IOException("http " + code);
                }
                Path parent = dest.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (InputStream in = connection.getInputStream();
                     OutputStream out = Files.newOutputStream(
                             dest,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING
                     )) {
                    in.transferTo(out);
                } catch (IOException exception) {
                    Files.deleteIfExists(dest);
                    throw exception;
                }
                return;
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("too many redirects");
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestMethod("GET");
        return connection;
    }
}
