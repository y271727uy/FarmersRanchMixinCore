package com.y271727uy.frmc.mixincoremodapi;

import com.y271727uy.frmc.mixinagent.Substratum;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;

import java.util.List;
import java.util.Set;

public final class MixinCoremodTransformationService implements ITransformationService {
    private static final String STATE_CLASS = "com.y271727uy.frmc.mixincoremodapi.CoremodState";

    @Override
    public String name() {
        return "frmc_coremod_api";
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) throws IncompatibleEnvironmentException {
        Substratum.install(true);
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public List<ITransformer> transformers() {
        return List.of(new StateTransformer(), Substratum.transformer());
    }

    private static final class StateTransformer implements ITransformer<ClassNode> {
        @Override
        public ClassNode transform(ClassNode target, ITransformerVotingContext context) {
            target.methods.stream()
                    .filter(method -> "isActive".equals(method.name) && "()Z".equals(method.desc))
                    .findFirst()
                    .ifPresent(method -> {
                        method.instructions.clear();
                        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
                        method.instructions.add(new InsnNode(Opcodes.IRETURN));
                        method.maxStack = 1;
                        method.maxLocals = 0;
                    });
            return target;
        }

        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return STATE_CLASS.equals(context.getClassName())
                    ? TransformerVoteResult.YES
                    : TransformerVoteResult.NO;
        }

        @Override
        public Set<Target> targets() {
            return Set.of(Target.targetClass(STATE_CLASS));
        }

        @Override
        public String[] labels() {
            return new String[]{"mixin-coremod-api"};
        }
    }
}
