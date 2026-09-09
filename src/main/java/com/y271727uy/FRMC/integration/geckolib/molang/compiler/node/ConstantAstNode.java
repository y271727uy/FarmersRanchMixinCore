package com.y271727uy.FRMC.integration.geckolib.molang.compiler.node;

import com.y271727uy.FRMC.integration.geckolib.molang.compiler.AstNode;
import com.y271727uy.FRMC.integration.geckolib.molang.compiler.BytecodeGen;
import org.objectweb.asm.commons.InstructionAdapter;

public record ConstantAstNode(double value) implements AstNode {
    @Override
    public double eval() {
        return this.value;
    }

    @Override
    public AstNode[] children() {
        return new AstNode[0];
    }

    @Override
    public void emit(BytecodeGen.Context context, InstructionAdapter method) {
        method.dconst(this.value);
    }
}

