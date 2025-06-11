package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.Type;

public sealed interface ExpressionTree extends Tree permits BinaryOperationTree, IdentExpressionTree, LiteralTree, NegateTree {
    default Type type() {
        System.out.println("ExpressionTree: default type() called, returning BasicType.BOOL");
        return BasicType.BOOL;}
}
