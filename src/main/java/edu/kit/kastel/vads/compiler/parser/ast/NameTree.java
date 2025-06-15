package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

public record NameTree(Name name, Span span) implements Tree {

    @Override
    public Span span() {
        return span;
    }
    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }


}
