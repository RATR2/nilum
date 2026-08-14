package io.github.r4t2.nilum.common.expr;

import java.util.ArrayList;
import java.util.List;

final class ExprLexer {

    private final String source;
    private int pos;

    private ExprLexer(String source) {
        this.source = source;
    }

    static List<ExprToken> tokenize(String source) {
        return new ExprLexer(source).run();
    }

    private List<ExprToken> run() {
        List<ExprToken> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= source.length()) {
                tokens.add(new ExprToken(ExprToken.Type.EOF, ""));
                return tokens;
            }

            char c = source.charAt(pos);
            if (Character.isDigit(c) || (c == '.' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1)))) {
                tokens.add(readNumber());
            } else if (c == '"') {
                tokens.add(readString());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdent());
            } else {
                tokens.add(readSymbol());
            }
        }
    }

    private void skipWhitespace() {
        while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
            pos++;
        }
    }

    private ExprToken readNumber() {
        int start = pos;
        while (pos < source.length() && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
            pos++;
        }
        return new ExprToken(ExprToken.Type.NUMBER, source.substring(start, pos));
    }

    private ExprToken readString() {
        pos++; // opening quote
        int start = pos;
        while (pos < source.length() && source.charAt(pos) != '"') {
            pos++;
        }
        if (pos >= source.length()) {
            throw new ExprParseException("Unterminated string literal");
        }
        String text = source.substring(start, pos);
        pos++; // closing quote
        return new ExprToken(ExprToken.Type.STRING, text);
    }

    private ExprToken readIdent() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos))
                || source.charAt(pos) == '_' || source.charAt(pos) == ':'
                || source.charAt(pos) == '.' || source.charAt(pos) == '#')) {
            pos++;
        }
        return new ExprToken(ExprToken.Type.IDENT, source.substring(start, pos));
    }

    private ExprToken readSymbol() {
        char c = source.charAt(pos);
        char next = pos + 1 < source.length() ? source.charAt(pos + 1) : '\0';

        return switch (c) {
            case '(' -> single(ExprToken.Type.LPAREN);
            case ')' -> single(ExprToken.Type.RPAREN);
            case ',' -> single(ExprToken.Type.COMMA);
            case '+' -> single(ExprToken.Type.PLUS);
            case '-' -> single(ExprToken.Type.MINUS);
            case '*' -> single(ExprToken.Type.STAR);
            case '/' -> single(ExprToken.Type.SLASH);
            case '%' -> single(ExprToken.Type.PERCENT);
            case '>' -> next == '=' ? doubleSym(ExprToken.Type.GE) : single(ExprToken.Type.GT);
            case '<' -> next == '=' ? doubleSym(ExprToken.Type.LE) : single(ExprToken.Type.LT);
            case '=' -> next == '=' ? doubleSym(ExprToken.Type.EQ)
                    : throwUnexpected(c);
            case '!' -> next == '=' ? doubleSym(ExprToken.Type.NE) : single(ExprToken.Type.NOT);
            case '&' -> next == '&' ? doubleSym(ExprToken.Type.AND) : throwUnexpected(c);
            case '|' -> next == '|' ? doubleSym(ExprToken.Type.OR) : throwUnexpected(c);
            default -> throwUnexpected(c);
        };
    }

    private ExprToken single(ExprToken.Type type) {
        pos++;
        return new ExprToken(type, "");
    }

    private ExprToken doubleSym(ExprToken.Type type) {
        pos += 2;
        return new ExprToken(type, "");
    }

    private ExprToken throwUnexpected(char c) {
        throw new ExprParseException("Unexpected character '" + c + "'");
    }
}
