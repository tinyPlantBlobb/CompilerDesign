package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;

public sealed interface ExpressionTree extends Tree permits BinaryOperationTree, IdentExpressionTree, LiteralTree, NegateTree, TernaryOperationTree, UnaryOperationTree {
    default Type type() {
        return BasicType.BOOL;}
}
