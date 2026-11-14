package com.y271727uy.FRMC.mixin.touhoulittlemaid;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.value.MolangValue;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.BinaryExpression;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.DoubleExpression;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.Expression;
import com.github.tartaricacid.touhoulittlemaid.molang.parser.ast.UnaryExpression;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MolangValue.class, remap = false)
public abstract class MolangValueMixin {
    @Unique private Object frmc$constant;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void frmc$foldConstant(List<Expression> expressions, CallbackInfo ci) {
        if (expressions.size() == 1 && frmc$isConstantArithmetic(expressions.get(0))) {
            frmc$constant = ExpressionEvaluator.evaluator().eval(expressions.get(0));
        }
    }

    @Inject(method = "evalUnsafe", at = @At("HEAD"), cancellable = true)
    private void frmc$readConstant(ExpressionEvaluator<?> evaluator, CallbackInfoReturnable<Object> cir) {
        if (frmc$constant != null) {
            Object pendingReturn = evaluator.popReturnValue();
            cir.setReturnValue(pendingReturn == null ? frmc$constant : pendingReturn);
        }
    }

    @Unique
    private static boolean frmc$isConstantArithmetic(Expression expression) {
        if (expression instanceof DoubleExpression) {
            return true;
        }
        if (expression instanceof UnaryExpression unary) {
            return (unary.op() == UnaryExpression.Op.PLUS || unary.op() == UnaryExpression.Op.ARITHMETICAL_NEGATION)
                    && frmc$isConstantArithmetic(unary.expression());
        }
        if (expression instanceof BinaryExpression binary) {
            return switch (binary.op()) {
                case ADD, SUB, MUL, DIV -> frmc$isConstantArithmetic(binary.left())
                        && frmc$isConstantArithmetic(binary.right());
                default -> false;
            };
        }
        return false;
    }
}
