package com.y271727uy.frmc.mixinagent;

import com.y271727uy.frmc.mixinasm.Bytecodes;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

final class SubstratumTransformer implements ITransformer<ClassNode> {
    private final SubstratumRegistry registry;

    SubstratumTransformer(SubstratumRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        if (input == null || Bytecodes.applied(input)) {
            return input;
        }
        try {
            if (registry.apply(input)) {
                Bytecodes.mark(input);
            }
        } catch (Throwable throwable) {
            System.err.println("[FRMC/substratum] transformer skipped " + input.name + ": " + throwable);
        }
        return input;
    }

    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        String name = context == null ? "" : Bytecodes.dotted(context.getClassName());
        return registry.matches(name) ? TransformerVoteResult.YES : TransformerVoteResult.NO;
    }

    @Override
    public Set<Target> targets() {
        Set<Target> targets = new HashSet<>();
        for (String name : registry.exactNames()) {
            targets.add(Target.targetClass(name));
        }
        return targets;
    }

    @Override
    public String[] labels() {
        return new String[]{"frmc-substratum"};
    }
}
