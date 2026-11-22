package com.y271727uy.FRMC.integration.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeneratedPackIndexTest {
    private static final List<String> NAMESPACES = List.of("minecraft", "kubejs", "farmersdelight", "a", "a_b");
    private static final List<String> FOLDERS = List.of("recipes", "recipe", "recipes_extra", "tags/items", "tags/item",
            "tags/blocks", "loot_tables/blocks", "advancements", "worldgen/biome", "worldgen");

    private static IoSupplier<InputStream> data(String text) {
        byte[] bytes = text.getBytes();
        return () -> new ByteArrayInputStream(bytes);
    }

    /** KubeJS 2001.6.5-build.26 GeneratedResourcePack#listResources, restated from its bytecode. */
    private static Map<ResourceLocation, IoSupplier<InputStream>> linear(Map<ResourceLocation, IoSupplier<InputStream>> generated,
                                                                     String namespace, String path) {
        String prefix = path.endsWith("/") ? path : path + "/";
        Map<ResourceLocation, IoSupplier<InputStream>> out = new HashMap<>();
        for (var entry : generated.entrySet()) {
            if (entry.getKey().getNamespace().equals(namespace) && entry.getKey().getPath().startsWith(prefix)) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    private static Map<ResourceLocation, IoSupplier<InputStream>> indexed(Map<ResourceLocation, IoSupplier<InputStream>> generated,
                                                                      GeneratedPackIndex index, String namespace, String path) {
        Map<ResourceLocation, IoSupplier<InputStream>> out = new HashMap<>();
        index.list(generated, namespace, path.endsWith("/") ? path : path + "/", (id, supplier) -> {
            if (out.put(id, supplier) != null) {
                throw new AssertionError("listed twice: " + id);
            }
        });
        return out;
    }

    @Test
    void matchesLinearScanOnRandomPacks() {
        Random random = new Random(20261115L);
        for (int round = 0; round < 50; round++) {
            Map<ResourceLocation, IoSupplier<InputStream>> generated = new HashMap<>();
            int files = random.nextInt(400);
            for (int i = 0; i < files; i++) {
                String namespace = NAMESPACES.get(random.nextInt(NAMESPACES.size()));
                String folder = FOLDERS.get(random.nextInt(FOLDERS.size()));
                String name = random.nextInt(4) == 0 ? folder + ".json" : folder + "/f" + random.nextInt(60) + ".json";
                if (random.nextInt(5) == 0) {
                    name = folder + "/sub" + random.nextInt(3) + "/g" + random.nextInt(20) + ".json";
                }
                generated.put(new ResourceLocation(namespace, name), data(name));
            }
            GeneratedPackIndex index = GeneratedPackIndex.build(generated);
            for (String namespace : NAMESPACES) {
                for (String folder : FOLDERS) {
                    for (String path : List.of(folder, folder + "/", folder + "/sub1")) {
                        assertEquals(linear(generated, namespace, path), indexed(generated, index, namespace, path),
                                () -> namespace + ":" + path);
                    }
                }
            }
            assertEquals(Map.of(), indexed(generated, index, "missing_namespace", "recipes"));
        }
    }

    @Test
    void prefixDoesNotMatchSiblingFolders() {
        Map<ResourceLocation, IoSupplier<InputStream>> generated = new HashMap<>();
        generated.put(new ResourceLocation("kubejs", "recipes/a.json"), data("a"));
        generated.put(new ResourceLocation("kubejs", "recipes_extra/b.json"), data("b"));
        generated.put(new ResourceLocation("kubejs", "recipes.json"), data("c"));
        generated.put(new ResourceLocation("kubejs", "recipe/d.json"), data("d"));
        GeneratedPackIndex index = GeneratedPackIndex.build(generated);
        assertEquals(Map.of(new ResourceLocation("kubejs", "recipes/a.json"), generated.get(new ResourceLocation("kubejs", "recipes/a.json"))),
                indexed(generated, index, "kubejs", "recipes"));
    }

    @Test
    void servesReplacedValuesAndDetectsResizedMaps() {
        Map<ResourceLocation, IoSupplier<InputStream>> generated = new HashMap<>();
        ResourceLocation id = new ResourceLocation("kubejs", "recipes/a.json");
        generated.put(id, data("old"));
        GeneratedPackIndex index = GeneratedPackIndex.build(generated);

        IoSupplier<InputStream> replacement = data("new");
        generated.put(id, replacement);
        assertSame(replacement, indexed(generated, index, "kubejs", "recipes").get(id));

        generated.put(new ResourceLocation("kubejs", "recipes/b.json"), data("b"));
        assertEquals(false, index.covers(generated));
        assertEquals(false, index.covers(new HashMap<>(generated)));
        assertEquals(true, GeneratedPackIndex.build(generated).covers(generated));
    }
}
