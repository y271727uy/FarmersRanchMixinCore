package com.y271727uy.FRMC.capability.data_group.matcher;

import com.y271727uy.FRMC.capability.data_group.target.MatchTarget;

/**
 * Matcher that determines if a target belongs to a data group.
 */
public interface GroupMatcher {
    /**
     * Tests if the given target matches this matcher's criteria.
     */
    boolean matches(MatchTarget target);

    /**
     * Returns a type identifier for this matcher (used in JSON serialization).
     */
    String getType();
}
