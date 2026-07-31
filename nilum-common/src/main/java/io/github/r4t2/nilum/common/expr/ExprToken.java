package io.github.r4t2.nilum.common.expr;

record ExprToken(Type type, String text) {

    enum Type {
        NUMBER, STRING, IDENT,
        LPAREN, RPAREN, COMMA,
        PLUS, MINUS, STAR, SLASH, PERCENT,
        GT, LT, GE, LE, EQ, NE,
        AND, OR, NOT,
        EOF
    }
}
