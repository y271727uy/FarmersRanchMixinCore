package com.y271727uy.FRMC.integration.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KubeJS's generated pack answers every folder listing by walking all generated entries and comparing namespace and
 * path prefix. A data pack reload lists every folder of every namespace, so the walk repeats thousands of times.
 * This index groups the ids by namespace, sorted by path, and finds a prefix with a binary search.
 *
 * <p>Values are read from the live map at listing time, so a replaced entry is never served stale. The index is
 * rebuilt when the map instance or its size changes. Listing order differs from the HashMap order, which resource
 * managers do not depend on: they collect listings into maps keyed by id.
 */
public final class GeneratedPackIndex {
    private static final ResourceLocation[] EMPTY = new ResourceLocation[0];

    private final Map<ResourceLocation, ?> source;
    private final int size;
    private final Map<String, ResourceLocation[]> byNamespace;

    private GeneratedPackIndex(Map<ResourceLocation, ?> source, int size, Map<String, ResourceLocation[]> byNamespace) {
        this.source = source;
        this.size = size;
        this.byNamespace = byNamespace;
    }

    public boolean covers(Map<ResourceLocation, ?> generated) {
        return source == generated && size == generated.size();
    }

    public static GeneratedPackIndex build(Map<ResourceLocation, ?> generated) {
        Map<String, List<ResourceLocation>> grouped = new HashMap<>();
        for (ResourceLocation id : generated.keySet()) {
            grouped.computeIfAbsent(id.getNamespace(), key -> new ArrayList<>()).add(id);
        }
        Map<String, ResourceLocation[]> byNamespace = new HashMap<>(grouped.size() * 2);
        grouped.forEach((namespace, ids) -> {
            ResourceLocation[] sorted = ids.toArray(EMPTY);
            Arrays.sort(sorted, Comparator.comparing(ResourceLocation::getPath));
            byNamespace.put(namespace, sorted);
        });
        return new GeneratedPackIndex(generated, generated.size(), byNamespace);
    }

    /** {@code prefix} must already end with {@code /}, as KubeJS normalises it. */
    @SuppressWarnings("unchecked")
    public void list(Map<ResourceLocation, ?> generated, String namespace, String prefix,
                     PackResources.ResourceOutput output) {
        ResourceLocation[] ids = byNamespace.get(namespace);
        if (ids == null) {
            return;
        }
        int low = 0;
        int high = ids.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ids[middle].getPath().compareTo(prefix) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        for (int i = low; i < ids.length && ids[i].getPath().startsWith(prefix); i++) {
            Object data = generated.get(ids[i]);
            if (data != null) {
                output.accept(ids[i], (IoSupplier<InputStream>) data);
            }
        }
    }
}
