package com.y271727uy.FRMC.integration.citadel.model;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Shares cached Citadel hierarchy data between rendering and pose validation. */
public final class CitadelSkeletonLayoutCache {
    private static final Map<AdvancedModelBox, WeakReference<Layout>> LAYOUTS = new WeakHashMap<>();
    private static final Map<String, Topology> TOPOLOGIES = new HashMap<>();
    private static final Map<BasicModelPart, WeakReference<BasicModelPart>> PARENTS = new WeakHashMap<>();
    private static long revision;

    private CitadelSkeletonLayoutCache() {
    }

    public static synchronized Layout getOrBuild(AdvancedModelBox root) {
        CitadelLayoutOwner owner = root instanceof CitadelLayoutOwner part ? part : null;
        WeakReference<Layout> reference = owner == null ? LAYOUTS.get(root) : null;
        Layout existing = owner != null ? owner.frmc$citadelLayout(revision)
                : reference != null ? reference.get() : null;
        if (existing != null) {
            return existing;
        }

        ObjectArrayList<BasicModelPart> nodes = new ObjectArrayList<>();
        IdentityHashMap<BasicModelPart, Integer> indices = new IdentityHashMap<>();
        IdentityHashMap<BasicModelPart, BasicModelPart> parentsByPart = new IdentityHashMap<>();
        collect(root, nodes, indices, parentsByPart);

        int[] parents = new int[nodes.size()];
        int[] depths = new int[nodes.size()];
        int maxDepth = 0;
        for (int i = 0; i < nodes.size(); i++) {
            BasicModelPart node = nodes.get(i);
            BasicModelPart parent = parentsByPart.get(node);
            if (parent != null) {
                PARENTS.put(node, new WeakReference<>(parent));
            }
            parents[i] = parent == null ? -1 : indices.getOrDefault(parent, -1);
            depths[i] = parents[i] < 0 ? 0 : depths[parents[i]] + 1;
            maxDepth = Math.max(maxDepth, depths[i]);
        }

        int[] levelCounts = new int[maxDepth + 1];
        for (int depth : depths) {
            levelCounts[depth]++;
        }
        int[] levelStarts = new int[levelCounts.length + 1];
        for (int level = 0; level < levelCounts.length; level++) {
            levelStarts[level + 1] = levelStarts[level] + levelCounts[level];
        }
        int[] levelCursor = levelStarts.clone();
        int[] nodesByLevel = new int[nodes.size()];
        for (int i = 0; i < depths.length; i++) {
            nodesByLevel[levelCursor[depths[i]]++] = i;
        }

        int[] nodeFlags = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            nodeFlags[i] = nodes.get(i) instanceof CitadelModelPart part ? part.frmc$layoutFlags() : 0;
        }
        Topology topology = TOPOLOGIES.computeIfAbsent(topologyKey(parents, depths, nodes, nodeFlags), ignored ->
                new Topology(parents, depths, nodesByLevel, levelStarts, nodeFlags));
        Layout layout = new Layout(nodes.toArray(new BasicModelPart[0]), topology, nodeFlags);
        if (owner != null) {
            owner.frmc$setCitadelLayout(revision, layout);
        } else {
            LAYOUTS.put(root, new WeakReference<>(layout));
        }
        return layout;
    }

    private static String topologyKey(int[] parents, int[] depths, ObjectArrayList<BasicModelPart> nodes,
                                      int[] nodeFlags) {
        StringBuilder key = new StringBuilder(Arrays.toString(parents)).append('|').append(Arrays.toString(depths))
                .append('|').append(Arrays.toString(nodeFlags));
        for (BasicModelPart node : nodes) {
            key.append('|').append(node.getClass().getName());
        }
        return key.toString();
    }

    private static void collect(BasicModelPart node,
                                ObjectArrayList<BasicModelPart> nodes,
                                IdentityHashMap<BasicModelPart, Integer> indices,
                                IdentityHashMap<BasicModelPart, BasicModelPart> parents) {
        if (indices.containsKey(node)) {
            return;
        }
        int index = nodes.size();
        indices.put(node, index);
        nodes.add(node);
        BasicModelPart[] children;
        if (node instanceof CitadelModelPart part) {
            children = part.frmc$childrenFast();
        } else if (node instanceof AdvancedModelBox advanced) {
            children = advanced.childModels.toArray(new BasicModelPart[0]);
        } else {
            return;
        }
        for (BasicModelPart child : children) {
            parents.put(child, node);
            collect(child, nodes, indices, parents);
        }
    }

    public static synchronized void invalidateAll() {
        revision++;
        LAYOUTS.clear();
        TOPOLOGIES.clear();
        PARENTS.clear();
    }

    public static synchronized BasicModelPart parentOf(BasicModelPart node) {
        WeakReference<BasicModelPart> parent = PARENTS.get(node);
        return parent != null ? parent.get() : null;
    }

    public static final class Layout {
        private final BasicModelPart[] nodes;
        private final Topology topology;
        private final int[] nodeFlags;

        private Layout(BasicModelPart[] nodes, Topology topology, int[] nodeFlags) {
            this.nodes = nodes;
            this.topology = topology;
            this.nodeFlags = nodeFlags;
        }

        public int nodeCount() {
            return nodes.length;
        }

        public BasicModelPart[] nodes() {
            return nodes;
        }

        public Topology topology() {
            return topology;
        }

        public int[] nodeFlags() {
            return nodeFlags;
        }


        public int[] parentIndices() {
            return topology.parentIndices;
        }

        public int[] depths() {
            return topology.depths;
        }

        public int[] nodesByLevel() {
            return topology.nodesByLevel;
        }

        /** Prefix ranges; level d occupies [levelStarts[d], levelStarts[d + 1]). */
        public int[] levelStarts() {
            return topology.levelStarts;
        }
    }

    public static final class Topology {
        private final int[] parentIndices;
        private final int[] depths;
        private final int[] nodesByLevel;
        private final int[] levelStarts;
        private final int[] matrixOffsets;
        private final int[] poseOffsets;
        private final int[] subtreeEnds;
        private final int[] nodeFlags;

        private Topology(int[] parentIndices, int[] depths, int[] nodesByLevel, int[] levelStarts,
                         int[] nodeFlags) {
            this.parentIndices = parentIndices;
            this.depths = depths;
            this.nodesByLevel = nodesByLevel;
            this.levelStarts = levelStarts;
            this.nodeFlags = nodeFlags;
            this.matrixOffsets = new int[parentIndices.length];
            this.poseOffsets = new int[parentIndices.length];
            this.subtreeEnds = new int[parentIndices.length];
            int[] stack = new int[depths.length];
            int stackSize = 0;
            for (int i = 0; i < depths.length; i++) {
                while (stackSize > 0 && depths[stack[stackSize - 1]] >= depths[i]) {
                    subtreeEnds[stack[--stackSize]] = i;
                }
                stack[stackSize++] = i;
                matrixOffsets[i] = i * 12;
                poseOffsets[i] = i * 10;
            }
            while (stackSize > 0) {
                subtreeEnds[stack[--stackSize]] = depths.length;
            }
        }

        public int nodeCount() {
            return parentIndices.length;
        }

        public int[] parentIndices() {
            return parentIndices;
        }

        public int[] depths() {
            return depths;
        }

        public int[] nodesByLevel() {
            return nodesByLevel;
        }

        public int[] matrixOffsets() {
            return matrixOffsets;
        }

        public int[] poseOffsets() {
            return poseOffsets;
        }

        public int[] levelStarts() {
            return levelStarts;
        }

        public int[] subtreeEnds() {
            return subtreeEnds;
        }

    }
}
