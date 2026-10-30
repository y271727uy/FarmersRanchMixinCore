package com.y271727uy.FRMC.capability.data_group;

import com.y271727uy.FRMC.capability.data_group.matcher.GroupMatcher;
import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A data group that matches items or blocks based on dynamic criteria.
 * Similar to vanilla tags but supports NBT/field matching.
 */
public final class DataGroup {
    private final ResourceLocation id;
    private final List<GroupMatcher> matchers;
    private final boolean replace;

    public DataGroup(ResourceLocation id, List<GroupMatcher> matchers, boolean replace) {
        this.id = id;
        this.matchers = new ArrayList<>(matchers);
        this.replace = replace;
    }

    /**
     * Tests if the given target matches any matcher in this group.
     */
    public boolean test(MatchTarget target) {
        for (GroupMatcher matcher : matchers) {
            if (matcher.matches(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a matcher to this group.
     */
    public void addMatcher(GroupMatcher matcher) {
        this.matchers.add(matcher);
    }

    /**
     * Returns the group ID.
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Returns an unmodifiable view of matchers.
     */
    public List<GroupMatcher> getMatchers() {
        return Collections.unmodifiableList(matchers);
    }

    /**
     * Returns true if this group replaces parent definitions.
     */
    public boolean isReplace() {
        return replace;
    }

    /**
     * Builder for creating data groups.
     */
    public static final class Builder {
        private final ResourceLocation id;
        private final List<GroupMatcher> matchers = new ArrayList<>();
        private boolean replace = false;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder replace(boolean replace) {
            this.replace = replace;
            return this;
        }

        public Builder add(GroupMatcher matcher) {
            this.matchers.add(matcher);
            return this;
        }

        public Builder addAll(List<GroupMatcher> matchers) {
            this.matchers.addAll(matchers);
            return this;
        }

        public DataGroup build() {
            return new DataGroup(id, matchers, replace);
        }
    }

    /**
     * Creates a new builder for this group ID.
     */
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }
}
