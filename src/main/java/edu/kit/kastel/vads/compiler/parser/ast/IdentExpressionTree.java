package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IdentExpressionTree(NameTree name) implements ExpressionTree {
    @Override
    public Type type() {

        System.out.println("IdentExpressionTree: " + name+".type() = " + name.references.type()+
                ", name = " + name.references);;
        return name.references.type();
    }

    @Override
    public Span span() {
        return name().span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}
