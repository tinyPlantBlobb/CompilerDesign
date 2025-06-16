package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import org.jspecify.annotations.NullMarked;

public record  IfTree(ExpressionTree condition, StatementTree thenTree, @NullMarked StatementTree elseTree, Position spanStart) implements ControlFlowTree {



    @Override
    public Span span() {
        if (elseTree == null) {
            return condition.span().merge(thenTree.span());
        }else {
        return condition.span().merge(thenTree.span())
                .merge( elseTree.span());
    }}

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
