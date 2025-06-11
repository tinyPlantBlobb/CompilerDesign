package edu.kit.kastel.vads.compiler.ir.node;

public final class LessThanOrEqualNode extends BinaryOperationNode {
    LessThanOrEqualNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
