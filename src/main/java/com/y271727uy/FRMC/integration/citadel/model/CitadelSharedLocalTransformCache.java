package com.y271727uy.FRMC.integration.citadel.model;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Bounded render-thread cache for exact Citadel local transforms shared by model instances.
 * Keys are the complete local position/rotation/scale bit pattern plus part kind.
 */
public final class CitadelSharedLocalTransformCache {
    private static final int CAPACITY = 512;
    private static final ThreadLocal<Workspace> WORKSPACE = ThreadLocal.withInitial(Workspace::new);

    private CitadelSharedLocalTransformCache() {
    }

    public static Entry lookup(int kind, int px, int py, int pz, int rx, int ry, int rz,
                               int sx, int sy, int sz) {
        Workspace workspace = WORKSPACE.get();
        int slot = hash(kind, px, py, pz, rx, ry, rz, sx, sy, sz) & (CAPACITY - 1);
        if (!workspace.valid[slot] || workspace.kinds[slot] != kind
                || workspace.keys[slot][0] != px || workspace.keys[slot][1] != py
                || workspace.keys[slot][2] != pz || workspace.keys[slot][3] != rx
                || workspace.keys[slot][4] != ry || workspace.keys[slot][5] != rz
                || workspace.keys[slot][6] != sx || workspace.keys[slot][7] != sy
                || workspace.keys[slot][8] != sz) {
            return null;
        }
        return workspace.entries[slot];
    }

    public static void store(int kind, int px, int py, int pz, int rx, int ry, int rz,
                             int sx, int sy, int sz, Matrix4f pose, Matrix3f normal) {
        Workspace workspace = WORKSPACE.get();
        int slot = hash(kind, px, py, pz, rx, ry, rz, sx, sy, sz) & (CAPACITY - 1);
        workspace.valid[slot] = true;
        workspace.kinds[slot] = kind;
        int[] key = workspace.keys[slot];
        key[0] = px; key[1] = py; key[2] = pz; key[3] = rx; key[4] = ry;
        key[5] = rz; key[6] = sx; key[7] = sy; key[8] = sz;
        workspace.entries[slot].pose.set(pose);
        workspace.entries[slot].normal.set(normal);
    }

    private static int hash(int kind, int px, int py, int pz, int rx, int ry, int rz,
                            int sx, int sy, int sz) {
        int hash = 0x811c9dc5 ^ kind;
        hash = (hash ^ px) * 0x01000193;
        hash = (hash ^ py) * 0x01000193;
        hash = (hash ^ pz) * 0x01000193;
        hash = (hash ^ rx) * 0x01000193;
        hash = (hash ^ ry) * 0x01000193;
        hash = (hash ^ rz) * 0x01000193;
        hash = (hash ^ sx) * 0x01000193;
        hash = (hash ^ sy) * 0x01000193;
        return (hash ^ sz) * 0x01000193;
    }

    public static final class Entry {
        public final Matrix4f pose = new Matrix4f();
        public final Matrix3f normal = new Matrix3f();
    }

    private static final class Workspace {
        private final boolean[] valid = new boolean[CAPACITY];
        private final int[] kinds = new int[CAPACITY];
        private final int[][] keys = new int[CAPACITY][9];
        private final Entry[] entries = new Entry[CAPACITY];

        private Workspace() {
            for (int i = 0; i < entries.length; i++) {
                entries[i] = new Entry();
            }
        }
    }
}
