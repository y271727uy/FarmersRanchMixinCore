package com.y271727uy.FRMC.capability.villager.diagnostics;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft-free naming helpers for anonymous declarative behaviors. */
public final class BehaviorNames {
    private static final Pattern LAMBDA_OWNER = Pattern.compile("([\\w$]+?)\\$\\$Lambda");
    private static final int MAX_DEBUG_NAME = 60;

    private BehaviorNames() {
    }

    /**
     * Declarative behaviors print their captured lambdas, whose class names start with the factory that built
     * them (for example "net.minecraft...AcquirePoi$$Lambda$123/0x..."). Returns those owners joined by '+',
     * or a shortened debug string when none are present.
     */
    public static String lambdaOwners(String debug) {
        Matcher matcher = LAMBDA_OWNER.matcher(debug);
        Set<String> owners = new LinkedHashSet<>();
        while (matcher.find()) {
            String owner = matcher.group(1);
            if (!owner.equals("BehaviorBuilder") && !owner.startsWith("BehaviorBuilder$")) {
                owners.add(owner);
            }
        }
        if (!owners.isEmpty()) {
            return String.join("+", owners);
        }
        return debug.length() > MAX_DEBUG_NAME ? debug.substring(0, MAX_DEBUG_NAME) + "..." : debug;
    }
}
