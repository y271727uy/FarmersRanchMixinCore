package com.y271727uy.FRMC.integration.geckolib.molang.compiler;

import com.eliotlash.mclib.math.IValue;

import java.util.List;

record CompiledAst(AstNode root, List<IValue> references) {
}

