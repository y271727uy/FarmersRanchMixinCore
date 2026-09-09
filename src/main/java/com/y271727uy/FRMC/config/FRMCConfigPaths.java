package com.y271727uy.FRMC.config;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class FRMCConfigPaths {
    private static final String DIRECTORY_NAME = "frmc";

    private FRMCConfigPaths() {}

    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(DIRECTORY_NAME);
    }

    public static Path resolve(String fileName) {
        return directory().resolve(fileName);
    }

    public static void ensureDirectory() {
        try {
            Files.createDirectories(directory());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create FRMC config directory " + directory(), exception);
        }
    }

    public static void ensureMusicListFile() {
        Path path = resolve("music-list.txt");
        if (Files.exists(path)) {
            return;
        }
        String template = "# One NetEase Cloud Music song URL per line.\n"
                + "# Exported lines may contain a title before or after the URL.\n"
                + "# Supported examples:\n"
                + "# https://music.163.com/song?id=123\n"
                + "# https://music.163.com/#/song?id=123\n"
                + "# https://music.163.com/#/dj?id=3720311521\n"
                + "https://music.163.com/#/dj?id=3720311521\n"
                + "# Playlist pages and 163cn.tv short links are ignored.\n";
        try {
            Files.writeString(path, template, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create FRMC music list " + path, exception);
        }
    }

    public static void migrateLegacy(String fileName) {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve(fileName);
        Path current = resolve(fileName);
        try {
            if (Files.exists(legacy) && !Files.exists(current)) {
                Files.move(legacy, current);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to migrate FRMC config " + legacy + " to " + current, exception);
        }
    }
}
