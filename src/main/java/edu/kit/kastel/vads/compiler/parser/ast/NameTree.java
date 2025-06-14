package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.Set;

public record NameTree(Name name, Span span) implements Tree {
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    public static DeclarationTree references = null;

    public void addReference(DeclarationTree declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("references cannot be null");
        }
        if (references == null) {references = declaration;}

    }
}
