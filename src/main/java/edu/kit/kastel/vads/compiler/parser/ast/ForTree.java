package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

public record ForTree(@NullMarked StatementTree initialisation, ExpressionTree condition, @NullMarked StatementTree step, StatementTree loopBody, Position spanStart) implements ControlFlowTree {

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

    @Override
    public Span span() {
        return new Span.SimpleSpan(spanStart, loopBody.span().end());
    }
}
