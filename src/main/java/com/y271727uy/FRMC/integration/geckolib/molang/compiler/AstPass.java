package com.y271727uy.FRMC.integration.geckolib.molang.compiler;

@FunctionalInterface
public interface AstPass {
    AstNode apply(AstNode node);
}

