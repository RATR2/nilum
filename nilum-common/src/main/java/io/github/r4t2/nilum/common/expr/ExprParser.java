package io.github.r4t2.nilum.common.expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for the HUD expression DSL. Parsed once at atlas load time into an
 * AST, never re-parsed per frame. Depth and total-node limits are safety backstops against a
 * pathological expression, not a security measure.
 */
public final class ExprParser {

    private static final int MAX_DEPTH = 64;
    private static final int MAX_NODES = 512;

    private final List<ExprToken> tokens;
    private int index;
    private int nodeCount;

    private ExprParser(List<ExprToken> tokens) {
        this.tokens = tokens;
    }

    public static ExprNode parse(String source) {
        ExprParser parser = new ExprParser(ExprLexer.tokenize(source));
        ExprNode node = parser.parseOr(0);
        parser.expect(ExprToken.Type.EOF, "end of expression");
        return node;
    }

    private ExprNode parseOr(int depth) {
        ExprNode left = parseAnd(depth);
        while (check(ExprToken.Type.OR)) {
            advance();
            left = node(depth, new ExprNode.BinaryOp("||", left, parseAnd(depth)));
        }
        return left;
    }

    private ExprNode parseAnd(int depth) {
        ExprNode left = parseEquality(depth);
        while (check(ExprToken.Type.AND)) {
            advance();
            left = node(depth, new ExprNode.BinaryOp("&&", left, parseEquality(depth)));
        }
        return left;
    }

    private ExprNode parseEquality(int depth) {
        ExprNode left = parseRelational(depth);
        while (check(ExprToken.Type.EQ) || check(ExprToken.Type.NE)) {
            String op = peek().type() == ExprToken.Type.EQ ? "==" : "!=";
            advance();
            left = node(depth, new ExprNode.BinaryOp(op, left, parseRelational(depth)));
        }
        return left;
    }

    private ExprNode parseRelational(int depth) {
        ExprNode left = parseAdditive(depth);
        while (check(ExprToken.Type.GT) || check(ExprToken.Type.LT)
                || check(ExprToken.Type.GE) || check(ExprToken.Type.LE)) {
            String op = switch (peek().type()) {
                case GT -> ">";
                case LT -> "<";
                case GE -> ">=";
                case LE -> "<=";
                default -> throw new IllegalStateException();
            };
            advance();
            left = node(depth, new ExprNode.BinaryOp(op, left, parseAdditive(depth)));
        }
        return left;
    }

    private ExprNode parseAdditive(int depth) {
        ExprNode left = parseMultiplicative(depth);
        while (check(ExprToken.Type.PLUS) || check(ExprToken.Type.MINUS)) {
            String op = peek().type() == ExprToken.Type.PLUS ? "+" : "-";
            advance();
            left = node(depth, new ExprNode.BinaryOp(op, left, parseMultiplicative(depth)));
        }
        return left;
    }

    private ExprNode parseMultiplicative(int depth) {
        ExprNode left = parseUnary(depth);
        while (check(ExprToken.Type.STAR) || check(ExprToken.Type.SLASH) || check(ExprToken.Type.PERCENT)) {
            String op = switch (peek().type()) {
                case STAR -> "*";
                case SLASH -> "/";
                case PERCENT -> "%";
                default -> throw new IllegalStateException();
            };
            advance();
            left = node(depth, new ExprNode.BinaryOp(op, left, parseUnary(depth)));
        }
        return left;
    }

    private ExprNode parseUnary(int depth) {
        if (check(ExprToken.Type.NOT) || check(ExprToken.Type.MINUS)) {
            String op = peek().type() == ExprToken.Type.NOT ? "!" : "-";
            advance();
            return node(depth, new ExprNode.UnaryOp(op, parsePrimary(nextDepth(depth))));
        }
        return parsePrimary(depth);
    }

    private ExprNode parsePrimary(int depth) {
        ExprToken token = peek();
        return switch (token.type()) {
            case NUMBER -> {
                advance();
                yield node(depth, new ExprNode.NumberLiteral(Double.parseDouble(token.text())));
            }
            case STRING -> {
                advance();
                yield node(depth, new ExprNode.StringLiteral(token.text()));
            }
            case IDENT -> parseFunctionCall(depth, token);
            case LPAREN -> {
                advance();
                ExprNode inner = parseOr(nextDepth(depth));
                expect(ExprToken.Type.RPAREN, "')'");
                yield inner;
            }
            default -> throw new ExprParseException("Unexpected token: " + token.type());
        };
    }

    private ExprNode parseFunctionCall(int depth, ExprToken nameToken) {
        advance();
        expect(ExprToken.Type.LPAREN, "'(' after '" + nameToken.text() + "'");

        List<ExprNode> arguments = new ArrayList<>();
        if (!check(ExprToken.Type.RPAREN)) {
            arguments.add(parseOr(nextDepth(depth)));
            while (check(ExprToken.Type.COMMA)) {
                advance();
                arguments.add(parseOr(nextDepth(depth)));
            }
        }
        expect(ExprToken.Type.RPAREN, "')' to close '" + nameToken.text() + "(...)'");

        return node(depth, new ExprNode.FunctionCall(nameToken.text(), arguments));
    }

    private int nextDepth(int depth) {
        int next = depth + 1;
        if (next > MAX_DEPTH) {
            throw new ExprParseException("Expression nesting exceeds the maximum depth of " + MAX_DEPTH);
        }
        return next;
    }

    private ExprNode node(int depth, ExprNode built) {
        nodeCount++;
        if (nodeCount > MAX_NODES) {
            throw new ExprParseException("Expression exceeds the maximum node count of " + MAX_NODES);
        }
        return built;
    }

    private ExprToken peek() {
        return tokens.get(index);
    }

    private boolean check(ExprToken.Type type) {
        return peek().type() == type;
    }

    private void advance() {
        index++;
    }

    private void expect(ExprToken.Type type, String description) {
        if (!check(type)) {
            throw new ExprParseException("Expected " + description + " but found " + peek().type());
        }
        advance();
    }
}
