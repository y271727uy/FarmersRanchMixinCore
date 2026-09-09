package com.y271727uy.FRMC.integration.geckolib.molang.compiler;

@FunctionalInterface
public interface AstTransformer {
    AstNode transform(AstNode node);
}

