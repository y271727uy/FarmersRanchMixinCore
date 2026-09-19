package com.y271727uy.FRMC.capability.downland;

import com.mojang.logging.LogUtils;
import com.y271727uy.FRMC.capability.downland.fetch.DlcDownloader;
import com.y271727uy.FRMC.capability.downland.fetch.DlcUrlFetcher;
import com.y271727uy.FRMC.capability.downland.index.DlcIndexFile;
import com.y271727uy.FRMC.capability.downland.index.DlcIndexParser;
import com.y271727uy.FRMC.capability.downland.install.DlcPaths;
import com.y271727uy.FRMC.capability.downland.install.DlcUnpacker;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackEntry;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackRules;
import com.y271727uy.FRMC.capability.downland.scan.DownloadPackScanner;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-side catalog of drop-in download archives under {@code <gamedir>/download}.
 * {@code .zip} and {@code .mrpack} are both zip containers. JAR {@code data/frmc/dlc}
 * packs are format samples only and are not listed here.
 */
public final class ContentPackRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<DownloadPackEntry> ENTRIES = new ArrayList<>();
    private static final ConcurrentHashMap<String, CompletableFuture<DlcDownloader.Result>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ExecutorService DOWNLOAD_POOL = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "frmc-dlc-download");
        thread.setDaemon(true);
        return thread;
    });

    private ContentPackRegistry() {}

    public record InstallJob(DlcUnpacker.Result unpack, CompletableFuture<DlcDownloader.Result> downloads) {
        public InstallJob {
            unpack = unpack == null ? DlcUnpacker.Result.failed(null, "pack is not installable") : unpack;
        }
    }

    public static Path downloadDirectory() {
        return FMLPaths.GAMEDIR.get().resolve(DownloadPackRules.DIRECTORY_NAME);
    }

    public static Path dlcDirectory() {
        return DlcPaths.root(FMLPaths.GAMEDIR.get());
    }

    public static void ensureDownloadDirectory() {
        Path directory = downloadDirectory();
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            LOGGER.warn("Failed to create download pack directory {}", directory, exception);
        }
    }

    public static void ensureDlcDirectory() {
        Path directory = dlcDirectory();
        try {
            DlcPaths.ensureRoot(FMLPaths.GAMEDIR.get());
        } catch (IOException exception) {
            LOGGER.warn("Failed to create DLC directory {}", directory, exception);
        }
    }

    public static synchronized List<DownloadPackEntry> reload() {
        ensureDownloadDirectory();
        ensureDlcDirectory();
        ENTRIES.clear();
        Path directory = downloadDirectory();
        if (Files.isDirectory(directory)) {
            ENTRIES.addAll(DownloadPackScanner.scan(directory));
        }
        LOGGER.info("Found {} download pack archive(s) in {}", ENTRIES.size(), directory);
        return all();
    }

    public static void load() {
        ensureDownloadDirectory();
        ensureDlcDirectory();
    }

    public static synchronized List<DownloadPackEntry> all() {
        return List.copyOf(ENTRIES);
    }

    public static synchronized DownloadPackEntry findByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        for (DownloadPackEntry entry : ENTRIES) {
            if (fileName.equals(entry.fileName())) {
                return entry;
            }
        }
        return null;
    }

    public static InstallJob install(DownloadPackEntry entry) {
        ensureDlcDirectory();
        DlcUnpacker.Result result = DlcUnpacker.unpack(entry, FMLPaths.GAMEDIR.get());
        switch (result.status()) {
            case SUCCESS -> LOGGER.info("Installed content pack {} to {}", entry.fileName(), result.packDir());
            case ALREADY_INSTALLED -> LOGGER.info("Content pack {} is already installed at {}", entry.fileName(), result.packDir());
            case REJECTED -> LOGGER.warn("Refused to install content pack {}: {}", entry == null ? "?" : entry.fileName(), result.detail());
            case FAILED -> LOGGER.warn("Failed to install content pack {}: {}", entry == null ? "?" : entry.fileName(), result.detail());
        }
        CompletableFuture<DlcDownloader.Result> downloads = null;
        if (result.status() == DlcUnpacker.Result.Status.SUCCESS
                || result.status() == DlcUnpacker.Result.Status.ALREADY_INSTALLED) {
            downloads = startRemoteDownload(result.packDir());
        }
        return new InstallJob(result, downloads);
    }

    public static boolean isDownloading(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }
        Path packDir = DlcPaths.packDir(FMLPaths.GAMEDIR.get(), packId);
        CompletableFuture<DlcDownloader.Result> future = IN_FLIGHT.get(flightKey(packDir));
        return future != null && !future.isDone();
    }

    private static CompletableFuture<DlcDownloader.Result> startRemoteDownload(Path packDir) {
        if (packDir == null) {
            return null;
        }
        String key = flightKey(packDir);
        synchronized (IN_FLIGHT) {
            CompletableFuture<DlcDownloader.Result> existing = IN_FLIGHT.get(key);
            if (existing != null && !existing.isDone()) {
                return existing;
            }
            DlcIndexFile index = DlcIndexParser.parsePack(packDir);
            if (index.failed()) {
                LOGGER.warn("Failed to parse DLC index in {}", packDir);
                return null;
            }
            if (index.source() == null) {
                LOGGER.info("DLC {} has no index JSON; local unpack only", packDir);
                return null;
            }
            LOGGER.info(
                    "DLC index {} queued {} download(s), skipped {}",
                    index.source().getFileName(),
                    index.files().size(),
                    index.skipped()
            );
            if (index.files().isEmpty()) {
                return null;
            }
            CompletableFuture<DlcDownloader.Result> future = DlcDownloader.downloadAsync(
                    packDir,
                    index,
                    DlcUrlFetcher.INSTANCE,
                    DOWNLOAD_POOL
            );
            IN_FLIGHT.put(key, future);
            future.whenComplete((download, error) -> {
                IN_FLIGHT.remove(key, future);
                if (error != null) {
                    LOGGER.warn("DLC download failed for {}", packDir, error);
                } else if (download == null) {
                    LOGGER.warn("DLC download returned no result for {}", packDir);
                } else {
                    LOGGER.info(
                            "DLC download {}: downloaded={} already={} failed={}",
                            packDir.getFileName(),
                            download.downloaded(),
                            download.alreadyPresent(),
                            download.failed()
                    );
                }
            });
            return future;
        }
    }

    private static String flightKey(Path packDir) {
        return packDir.toAbsolutePath().normalize().toString();
    }
}
