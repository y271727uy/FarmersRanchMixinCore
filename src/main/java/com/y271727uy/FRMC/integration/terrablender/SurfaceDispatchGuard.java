package com.y271727uy.FRMC.integration.terrablender;

import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Checks necessary conditions from the already compiled rules, without querying the biome. */
final class SurfaceDispatchGuard {
    private static final int MAX_GATES = 8;
    private final SurfaceRules.Condition[] gates;

    private SurfaceDispatchGuard(Set<SurfaceRules.Condition> gates) {
        this.gates = gates.toArray(SurfaceRules.Condition[]::new);
    }

    static SurfaceDispatchGuard create(Collection<SurfaceRules.SurfaceRule> namespaceRules) {
        Set<SurfaceRules.Condition> gates = new LinkedHashSet<>();
        for (SurfaceRules.SurfaceRule rule : namespaceRules) {
            if (!collectGates(rule, gates)) {
                return null;
            }
        }
        return new SurfaceDispatchGuard(gates);
    }

    private static boolean collectGates(SurfaceRules.SurfaceRule rule, Set<SurfaceRules.Condition> gates) {
        if (rule instanceof SurfaceRules.SequenceRule sequence) {
            for (SurfaceRules.SurfaceRule branch : sequence.rules()) {
                if (!collectGates(branch, gates)) {
                    return false;
                }
            }
            return true;
        }
        if (rule instanceof SurfaceRules.TestRule test && isBiomeIndependentGate(test.condition())) {
            // A false outer test prevents its entire branch from running, including custom follow-up rules.
            gates.add(test.condition());
            return gates.size() <= MAX_GATES;
        }
        return false;
    }

    private static boolean isBiomeIndependentGate(SurfaceRules.Condition condition) {
        // These exact 1.20.1 vanilla implementations read terrain or positional noise only.
        return switch (condition.getClass().getName()) {
            case "net.minecraft.world.level.levelgen.SurfaceRules$Context$AbovePreliminarySurfaceCondition",
                 "net.minecraft.world.level.levelgen.SurfaceRules$StoneDepthCheck$1StoneDepthCondition",
                 "net.minecraft.world.level.levelgen.SurfaceRules$VerticalGradientConditionSource$1VerticalGradientCondition" -> true;
            default -> false;
        };
    }

    boolean canSkipDispatch() {
        for (SurfaceRules.Condition gate : gates) {
            if (gate.test()) {
                return false;
            }
        }
        return true;
    }
}
