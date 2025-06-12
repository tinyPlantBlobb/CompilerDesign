package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record Keyword(KeywordType type, Span span) implements Token {
    @Override
    public boolean isKeyword(KeywordType keywordType) {
        return type() == keywordType;
    }

    @Override
    public boolean isControlFlow() {
        return type() == KeywordType.IF || type() == KeywordType.ELSE || type() == KeywordType.WHILE
                || type() == KeywordType.FOR || type() == KeywordType.CONTINUE || type() == KeywordType.BREAK
                || type() == KeywordType.RETURN;
    }

    @Override
    public String asString() {
        return type().keyword();
    }
}
