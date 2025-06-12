package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record UnaryOperationTree(Operator operand, ExpressionTree expression) implements ExpressionTree {
    @Override
    public Type type() {
        if (operand.type().equals(Operator.OperatorType.MINUS) || operand.type().equals(Operator.OperatorType.BITWISE_NOT)) {
            return BasicType.INT;
        }else if (operand.type().equals(Operator.OperatorType.LOGICAL_NOT)) {
            return BasicType.BOOL;
        }
        throw new IllegalArgumentException("Unsupported unary operator: " + operand.type());
    }

    @Override
    public Span span() {
        return operand().span().merge(expression().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
