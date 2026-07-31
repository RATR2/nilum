package io.github.r4t2.nilum.common.expr;

import java.util.List;

/**
 * AST for the client-side HUD expression language. Everything ultimately evaluates to a
 * {@code double} - booleans and comparisons coerce to 1.0/0.0, matching how the design doc's
 * {@code boolean(...)} value sources and comparison operators are consumed by numeric functions
 * like {@code if(...)} and {@code map(...)}.
 */
public sealed interface ExprNode {

    record NumberLiteral(double value) implements ExprNode {
    }

    /** Only ever meaningful as an argument to value-source calls, e.g. {@code number("minecraft:health")}. */
    record StringLiteral(String value) implements ExprNode {
    }

    /** Covers both value sources ({@code number}, {@code boolean}) and built-ins (round, map, if, ...). */
    record FunctionCall(String name, List<ExprNode> arguments) implements ExprNode {
    }

    record BinaryOp(String operator, ExprNode left, ExprNode right) implements ExprNode {
    }

    record UnaryOp(String operator, ExprNode operand) implements ExprNode {
    }
}
