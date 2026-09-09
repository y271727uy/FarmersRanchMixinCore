package com.y271727uy.FRMC.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuration for FRMC's integrated SmoothBoot thread tuning. */
public final class SmoothBootConfig {
    public ThreadCount threadCount = new ThreadCount();
    public ThreadPriority threadPriority = new ThreadPriority();

    public void validate() {
        threadCount.bootstrap = Math.max(1, threadCount.bootstrap);
        threadCount.main = Math.max(1, threadCount.main);
        threadPriority.game = Mth.clamp(threadPriority.game, 1, 10);
        threadPriority.integratedServer = Mth.clamp(threadPriority.integratedServer, 1, 10);
        threadPriority.bootstrap = Mth.clamp(threadPriority.bootstrap, 1, 10);
        threadPriority.main = Mth.clamp(threadPriority.main, 1, 10);
        threadPriority.io = Mth.clamp(threadPriority.io, 1, 10);
        threadPriority.modLoading = Mth.clamp(threadPriority.modLoading, 1, 10);
    }

    public static SmoothBootConfig load() {
        Path path = Path.of(System.getProperty("user.dir"), "config", "smoothboot.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            SmoothBootConfig config = Files.exists(path)
                ? gson.fromJson(Files.readString(path), SmoothBootConfig.class)
                : new SmoothBootConfig();
            if (config == null) config = new SmoothBootConfig();
            config.validate();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config));
            return config;
        } catch (IOException | RuntimeException exception) {
            SmoothBootConfig config = new SmoothBootConfig();
            config.validate();
            return config;
        }
    }

    public static final class ThreadCount {
        public int bootstrap = 1;
        public int main = 15;
    }

    public static final class ThreadPriority {
        public int game = 5;
        public int bootstrap = 1;
        public int main = 1;
        public int io = 1;
        public int integratedServer = 5;
        public int modLoading = 1;
    }
}
