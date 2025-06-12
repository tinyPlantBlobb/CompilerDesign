package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record TernaryOperationTree(ExpressionTree condition, ExpressionTree thenExpression,
                                   ExpressionTree elseExpression) implements ExpressionTree {
    @Override
    public Type type() {
        return thenExpression.type();
    }

    @Override
    public Span span() {
        return condition.span().merge(thenExpression.span().merge(elseExpression.span()));
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }


}
