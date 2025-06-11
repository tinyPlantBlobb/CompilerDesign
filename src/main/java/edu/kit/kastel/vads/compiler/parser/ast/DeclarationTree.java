package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import org.jspecify.annotations.Nullable;

public record DeclarationTree(TypeTree typetree, NameTree name, @Nullable ExpressionTree initializer) implements StatementTree {
    public Type type() {
        System.out.println("DeclarationTree "+name+": " + typetree + ".type() = " + typetree.type());

        return typetree.type();
    }

    @Override
    public Span span() {
        if (initializer() != null) {
            return typetree().span().merge(initializer().span());
        }
        return typetree().span().merge(name().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
