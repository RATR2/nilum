package io.github.r4t2.nilum.common.expr;

import java.util.List;

/**
 * Walks a parsed {@link ExprNode} tree every render tick. {@code counter}/{@code pulse} are pure
 * functions of {@code timeSeconds} (not internal mutable state), so evaluation has no history to
 * carry between calls - the same tree can be re-evaluated from scratch each tick.
 */
public final class ExprEvaluator {

    private ExprEvaluator() {
    }

    public static double evaluate(ExprNode node, ValueSource valueSource, double timeSeconds) {
        return switch (node) {
            case ExprNode.NumberLiteral n -> n.value();
            case ExprNode.StringLiteral s ->
                    throw new ExprEvaluationException("String literal used outside a value-source call: \"" + s.value() + "\"");
            case ExprNode.UnaryOp u -> evaluateUnary(u, valueSource, timeSeconds);
            case ExprNode.BinaryOp b -> evaluateBinary(b, valueSource, timeSeconds);
            case ExprNode.FunctionCall f -> evaluateCall(f, valueSource, timeSeconds);
        };
    }

    private static double evaluateUnary(ExprNode.UnaryOp node, ValueSource valueSource, double timeSeconds) {
        double operand = evaluate(node.operand(), valueSource, timeSeconds);
        return switch (node.operator()) {
            case "!" -> operand == 0 ? 1 : 0;
            case "-" -> -operand;
            default -> throw new ExprEvaluationException("Unknown unary operator: " + node.operator());
        };
    }

    private static double evaluateBinary(ExprNode.BinaryOp node, ValueSource valueSource, double timeSeconds) {
        // && and || short-circuit - the right side may be an expensive or invalid call the
        // author never intended to run when the left side already decides the result.
        if (node.operator().equals("&&")) {
            return truthy(evaluate(node.left(), valueSource, timeSeconds))
                    && truthy(evaluate(node.right(), valueSource, timeSeconds)) ? 1 : 0;
        }
        if (node.operator().equals("||")) {
            return truthy(evaluate(node.left(), valueSource, timeSeconds))
                    || truthy(evaluate(node.right(), valueSource, timeSeconds)) ? 1 : 0;
        }

        double left = evaluate(node.left(), valueSource, timeSeconds);
        double right = evaluate(node.right(), valueSource, timeSeconds);
        return switch (node.operator()) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> left / right;
            case "%" -> left % right;
            case ">" -> left > right ? 1 : 0;
            case "<" -> left < right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            case "==" -> left == right ? 1 : 0;
            case "!=" -> left != right ? 1 : 0;
            default -> throw new ExprEvaluationException("Unknown binary operator: " + node.operator());
        };
    }

    private static double evaluateCall(ExprNode.FunctionCall node, ValueSource valueSource, double timeSeconds) {
        List<ExprNode> args = node.arguments();
        return switch (node.name()) {
            case "number", "boolean" -> valueSource.resolve(requireStringArg(node, args, 0));

            case "round" -> Math.round(arg(args, valueSource, timeSeconds, 0));
            case "floor" -> Math.floor(arg(args, valueSource, timeSeconds, 0));
            case "ceil" -> Math.ceil(arg(args, valueSource, timeSeconds, 0));

            case "clamp" -> {
                double value = arg(args, valueSource, timeSeconds, 0);
                double min = arg(args, valueSource, timeSeconds, 1);
                double max = arg(args, valueSource, timeSeconds, 2);
                yield Math.max(min, Math.min(max, value));
            }

            case "map" -> {
                double value = arg(args, valueSource, timeSeconds, 0);
                double inMin = arg(args, valueSource, timeSeconds, 1);
                double inMax = arg(args, valueSource, timeSeconds, 2);
                double outMin = arg(args, valueSource, timeSeconds, 3);
                double outMax = arg(args, valueSource, timeSeconds, 4);
                double t = inMax == inMin ? 0 : (value - inMin) / (inMax - inMin);
                yield outMin + t * (outMax - outMin);
            }

            case "if" -> {
                boolean condition = truthy(arg(args, valueSource, timeSeconds, 0));
                yield condition ? arg(args, valueSource, timeSeconds, 1) : arg(args, valueSource, timeSeconds, 2);
            }

            case "counter" -> {
                double min = arg(args, valueSource, timeSeconds, 0);
                double max = arg(args, valueSource, timeSeconds, 1);
                double interval = args.size() > 2 ? arg(args, valueSource, timeSeconds, 2) : 1.0;
                double span = max - min + 1;
                yield min + Math.floor(timeSeconds / interval) % span;
            }

            case "pulse" -> {
                double frameA = arg(args, valueSource, timeSeconds, 0);
                double frameB = arg(args, valueSource, timeSeconds, 1);
                double interval = arg(args, valueSource, timeSeconds, 2);
                boolean even = ((long) Math.floor(timeSeconds / interval)) % 2 == 0;
                yield even ? frameA : frameB;
            }

            default -> throw new ExprEvaluationException("Unknown function: " + node.name());
        };
    }

    private static double arg(List<ExprNode> args, ValueSource valueSource, double timeSeconds, int index) {
        if (index >= args.size()) {
            throw new ExprEvaluationException("Missing argument " + index);
        }
        return evaluate(args.get(index), valueSource, timeSeconds);
    }

    private static String requireStringArg(ExprNode.FunctionCall node, List<ExprNode> args, int index) {
        if (index >= args.size() || !(args.get(index) instanceof ExprNode.StringLiteral literal)) {
            throw new ExprEvaluationException(node.name() + "(...) requires a string literal argument");
        }
        return literal.value();
    }

    private static boolean truthy(double value) {
        return value != 0;
    }
}
